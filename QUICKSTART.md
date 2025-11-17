# AT-Google-Gemini Quick Start Guide

Get your AI-powered voice and SMS system up and running in minutes!

## Prerequisites

- Java 17 or higher
- Scala 2.13
- SBT 1.x
- Docker (for deployment)
- Africa's Talking account
- Google Gemini API key

## Step 1: Clone and Setup

```bash
# Clone the repository
git clone <your-repo-url>
cd at-google-gemini

# Copy environment template
cp .env.example .env
```

## Step 2: Get API Keys

### Africa's Talking
1. Sign up at [Africa's Talking](https://africastalking.com/)
2. Go to your [dashboard](https://account.africastalking.com/)
3. Copy your **API Key** and **Username**

### Google Gemini
1. Visit [Google AI Studio](https://ai.google.dev/)
2. Click "Get API Key"
3. Create a new project and copy your API key

## Step 3: Configure Environment

Edit `.env` with your credentials:

```bash
# Africa's Talking
AT_API_KEY=your_actual_api_key_here
AT_USERNAME=your_username_here

# Google Gemini
GEMINI_API_KEY=your_gemini_api_key_here

# Server (optional, defaults are fine)
PORT=8080
INTERFACE=0.0.0.0
```

## Step 4: Run Locally

```bash
# Load environment variables
export $(cat .env | xargs)

# Run the application
sbt run
```

You should see:
```
at-google-gemini service started at http://0.0.0.0:8080/
Webhook endpoint: http://0.0.0.0:8080/webhooks/voice/events
Health check: http://0.0.0.0:8080/admin/health
```

## Step 5: Test with Ngrok

To test webhooks locally, expose your server to the internet:

```bash
# Install ngrok (if not already installed)
# macOS: brew install ngrok
# Or download from: https://ngrok.com/download

# Start ngrok
ngrok http 8080
```

Copy the HTTPS URL (e.g., `https://abc123.ngrok.io`)

## Step 6: Configure Africa's Talking

1. Go to [Africa's Talking Dashboard](https://account.africastalking.com/)
2. Navigate to **Voice** or **SMS** settings
3. Set your callback URLs (use the API v1 endpoints):
   - **Voice Callback URL**: `https://abc123.ngrok.io/api/v1/webhooks/voice/events`
   - **SMS Callback URL**: `https://abc123.ngrok.io/api/v1/webhooks/sms/incoming`


## Step 7: Test Your System

### Test Voice AI
1. Call your Africa's Talking phone number
2. You should hear: "Hello! How can I help you today?"
3. Speak your question
4. The AI will respond!

### Test SMS AI
1. Send an SMS to your Africa's Talking number
2. The AI will respond via SMS

## Step 8: View Logs

```bash
# In your terminal running the app, you'll see:
[INFO] SMS from +254712345678: Hello
[INFO] AI Response: Hi! How can I help you today?
[INFO] Call started for session: call-abc123
```

## Step 9: Deploy to Production

### Deploy to Google Cloud Run

```bash
# Set your GCP project
export GCP_PROJECT_ID=your-project-id

# Run the deployment script
./deploy.sh
```

The script will:
- Build a Docker image
- Push to Google Container Registry
- Deploy to Cloud Run
- Verify the deployment

### Update Production Webhooks

After deployment, update Africa's Talking callbacks to your Cloud Run URL:
```
https://your-service-abc123.run.app/api/v1/webhooks/voice/events
https://your-service-abc123.run.app/api/v1/webhooks/sms/incoming
```

## Troubleshooting

### Server won't start
```bash
# Check if port 8080 is in use
lsof -i :8080

# Use a different port
export PORT=8081
sbt run
```

### Webhooks not working
- Verify your ngrok URL is HTTPS
- Check Africa's Talking dashboard for webhook logs
- View your server logs for incoming requests

### API errors
- Verify API keys are correct in `.env`
- Check API key permissions (Voice/SMS enabled)
- Ensure Gemini API key is activated

## Next Steps

1. **Customize AI Behavior**: Edit the system prompt in `CallSessionActor.scala` or `SMSConversationActor.scala`
2. **Add Features**: Implement DTMF menus, call transfers, or SMS commands
3. **Set up Monitoring**: Deploy Prometheus and Grafana (see Module 7 in README)
4. **Scale**: Configure auto-scaling in Cloud Run

## API Endpoints

### API Version 1 Endpoints (Use These)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/` | GET | Service info & API versions |
| `/api/v1/webhooks/voice/events` | POST | Voice call webhooks |
| `/api/v1/webhooks/sms/incoming` | POST | Incoming SMS webhooks |
| `/api/v1/webhooks/sms/delivery-reports` | POST | SMS delivery reports |

### Infrastructure Endpoints (Non-Versioned)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/admin/health` | GET | Health check |
| `/admin/health/ready` | GET | Readiness probe |
| `/admin/metrics/system` | GET | System metrics |
| `/admin/version` | GET | Version info |
| `/metrics` | GET | Prometheus metrics |


**Happy Building! 🚀**
