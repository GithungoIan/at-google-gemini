#!/bin/bash
# deploy.sh - Deploy VoiceAI to Google Cloud Run

set -e

PROJECT_ID="${GCP_PROJECT_ID:-your-gcp-project-id}"
SERVICE_NAME="${SERVICE_NAME:-voiceai-service}"
REGION="${GCP_REGION:-us-central1}"

echo "======================================"
echo "Deploying VoiceAI to Google Cloud Run"
echo "======================================"
echo ""
echo "Project ID: $PROJECT_ID"
echo "Service Name: $SERVICE_NAME"
echo "Region: $REGION"
echo ""

# Check if gcloud is installed
if ! command -v gcloud &> /dev/null; then
    echo "Error: gcloud CLI is not installed"
    echo "Install from: https://cloud.google.com/sdk/docs/install"
    exit 1
fi

# Check if docker is installed
if ! command -v docker &> /dev/null; then
    echo "Error: Docker is not installed"
    exit 1
fi

echo "[1/5] Building Docker image..."
docker build -t gcr.io/$PROJECT_ID/$SERVICE_NAME:latest .

echo ""
echo "[2/5] Pushing to Google Container Registry..."
docker push gcr.io/$PROJECT_ID/$SERVICE_NAME:latest

echo ""
echo "[3/5] Deploying to Cloud Run..."
gcloud run deploy $SERVICE_NAME \
  --image gcr.io/$PROJECT_ID/$SERVICE_NAME:latest \
  --platform managed \
  --region $REGION \
  --allow-unauthenticated \
  --memory 1Gi \
  --cpu 2 \
  --timeout 300 \
  --concurrency 80 \
  --min-instances 1 \
  --max-instances 100 \
  --set-env-vars "ENVIRONMENT=production" \
  --set-secrets "AT_API_KEY=at-api-key:latest,AT_USERNAME=at-username:latest,GEMINI_API_KEY=gemini-api-key:latest"

echo ""
echo "[4/5] Getting service URL..."
SERVICE_URL=$(gcloud run services describe $SERVICE_NAME --region $REGION --format 'value(status.url)')

echo ""
echo "[5/5] Verifying deployment..."
HEALTH_CHECK=$(curl -s -o /dev/null -w "%{http_code}" $SERVICE_URL/admin/health)

if [ "$HEALTH_CHECK" -eq 200 ]; then
    echo "✅ Health check passed!"
else
    echo "⚠️  Health check failed with status: $HEALTH_CHECK"
fi

echo ""
echo "======================================"
echo "Deployment Complete!"
echo "======================================"
echo ""
echo "Service URL: $SERVICE_URL"
echo "Health Check: $SERVICE_URL/admin/health"
echo "Webhooks:"
echo "  - Voice: $SERVICE_URL/webhooks/voice/events"
echo "  - SMS: $SERVICE_URL/webhooks/sms/incoming"
echo ""
echo "Next steps:"
echo "1. Update your Africa's Talking callback URLs to point to:"
echo "   Voice (v1): $SERVICE_URL/api/v1/webhooks/voice/events"
echo "   SMS (v1): $SERVICE_URL/api/v1/webhooks/sms/incoming"
echo ""
echo "   Legacy (deprecated, but still works):"
echo "   Voice: $SERVICE_URL/webhooks/voice/events"
echo "   SMS: $SERVICE_URL/webhooks/sms/incoming"
echo ""
echo "2. Monitor logs with:"
echo "   gcloud logging read \"resource.type=cloud_run_revision AND resource.labels.service_name=$SERVICE_NAME\" --limit 50"
echo ""
echo "3. View metrics in Cloud Console:"
echo "   https://console.cloud.google.com/run/detail/$REGION/$SERVICE_NAME"
echo ""
