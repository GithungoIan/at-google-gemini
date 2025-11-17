# API Versioning Strategy

## Overview

at-google-gemini implements **URL-based versioning** as the primary versioning strategy. This document explains our versioning approach, migration strategies, and best practices.

## Why API Versioning?

- **Backward Compatibility**: Allow existing integrations to continue working while introducing new features
- **Clear Communication**: Explicit version in URL makes it obvious which API version is being used
- **Gradual Migration**: Clients can migrate at their own pace
- **Breaking Changes**: Safely introduce breaking changes in new versions

## Versioning Strategy

### URL-Based Versioning (Primary)

All API endpoints include the version in the URL path:

```
/api/{version}/resource
```

**Examples:**
- `/api/v1/webhooks/voice/events` - Version 1 voice webhooks
- `/api/v2/webhooks/voice/events` - Future Version 2 (when needed)

### Version Format

- Format: `v{major}` (e.g., `v1`, `v2`, `v3`)
- Only major versions in URL
- Minor/patch changes are backward compatible within a major version

## Current Versions

### Version 1 (v1) - Current

**Status**: ✅ Stable, Production-Ready

**Endpoints:**
- `/api/v1/webhooks/voice/events`
- `/api/v1/webhooks/voice/dtmf`
- `/api/v1/webhooks/sms/incoming`
- `/api/v1/webhooks/sms/delivery-reports`

**Features:**
- Voice call handling with AI
- SMS conversation handling
- DTMF input support
- Delivery report tracking

### Version 2 (v2) - Future

**Status**: 🚧 Not Yet Available

Will be introduced when breaking changes are required. Currently returns:

```
HTTP 501 Not Implemented
{
  "error": "API v2 is not yet available. Please use /api/v1/"
}
```

**Planned for v2 (when needed):**
- Enhanced webhook payload structure
- Additional authentication methods
- Extended metadata support

### Legacy (No Version Prefix) - Deprecated

**Status**: ⚠️ Deprecated (Sunset: 2026-06-01)

**Endpoints:**
- `/webhooks/voice/events`
- `/webhooks/sms/incoming`

**Deprecation Headers:**
```http
Deprecation: true
Sunset: 2026-06-01
Link: </api/v1/webhooks>; rel="alternate"
X-API-Warn: API version legacy is deprecated. Please migrate to /api/v1/webhooks by 2026-06-01
```

## Version Lifecycle

### 1. Introduction Phase

- New version is announced
- Documentation is published
- Beta testing period (optional)
- Official release date set

### 2. Active Phase

- Version is stable and recommended
- Receives all new features
- Full support and bug fixes
- Performance optimizations

### 3. Maintenance Phase

- Version is stable but not actively developed
- Security and critical bug fixes only
- Deprecation announced
- Migration guide published
- Sunset date announced (minimum 12 months notice)

### 4. Sunset Phase

- Version is deprecated
- Deprecation headers added to all responses
- Active communication to users
- Migration support provided

### 5. End of Life

- Version is removed
- All requests return `410 Gone`
- Redirect to latest version (where possible)

## Migration Process

### For API Consumers (Africa's Talking Integration)

#### Step 1: Test New Version

```bash
# Test the new endpoint with your integration
curl -X POST https://your-domain.com/api/v1/webhooks/sms/incoming \
  -d "from=+254712345678" \
  -d "text=test"
```

#### Step 2: Update Webhook URLs

1. Log in to [Africa's Talking Dashboard](https://account.africastalking.com/)
2. Navigate to Voice/SMS Settings
3. Update callback URLs:
   - Old: `https://your-domain.com/webhooks/voice/events`
   - New: `https://your-domain.com/api/v1/webhooks/voice/events`

#### Step 3: Monitor

- Check for deprecation headers in responses
- Monitor application logs
- Verify functionality

#### Step 4: Complete Migration

- Remove references to old endpoints
- Update documentation
- Inform stakeholders

### Example: Legacy to v1 Migration

**Before:**
```javascript
const webhookUrl = 'https://myapp.com/webhooks/voice/events';
```

**After:**
```javascript
const webhookUrl = 'https://myapp.com/api/v1/webhooks/voice/events';
```

## Backward Compatibility

### What Requires a New Version?

Breaking changes that require a new major version:

- ❌ Removing fields from response
- ❌ Changing field types (string → number)
- ❌ Changing required vs optional fields
- ❌ Renaming fields
- ❌ Changing authentication method
- ❌ Changing URL structure (beyond version)

### What Doesn't Require a New Version?

Non-breaking changes (can be done in the same version):

- ✅ Adding new optional fields to response
- ✅ Adding new optional request parameters
- ✅ Adding new endpoints
- ✅ Improving performance
- ✅ Bug fixes
- ✅ Better error messages
- ✅ Internal refactoring

## Response Envelope Pattern

Consistent response structure across all versions:

```json
{
  "version": "1.0",
  "timestamp": "2025-01-17T10:30:00Z",
  "data": {
    // Version-specific payload
  },
  "meta": {
    "request_id": "req_abc123"
  }
}
```

## Header-Based Version Selection (Alternative)

While we primarily use URL-based versioning, header-based versioning is available:

```http
POST /webhooks/voice/events
API-Version: 1
```

**Response:**
```http
HTTP/1.1 200 OK
API-Version: 1
Content-Type: text/xml
```


## Version Support Policy

- **Current Version (v1)**: Full support, all features
- **Previous Version (Legacy)**: Security fixes only for 12 months
- **Deprecated Versions**: 6-month sunset period
- **Minimum Support Period**: 18 months from deprecation announcement

## Communication Plan

### Deprecation Announcement

When we deprecate a version:

1. **Email Notification**: All registered users
2. **Dashboard Notice**: Africa's Talking dashboard
3. **Deprecation Headers**: Added to all responses
4. **Documentation Update**: Clear notices in docs
5. **Blog Post**: Detailed announcement
6. **Migration Guide**: Step-by-step instructions

### Timeline Example

```
Month 0: v2 Released
  ├─ v1 remains fully supported
  └─ Users can begin testing v2

Month 6: v1 Deprecation Announced
  ├─ Deprecation headers added
  ├─ Email notifications sent
  └─ Sunset date set (Month 18)

Month 12: Active Migration Support
  ├─ Migration guide published
  ├─ Support team available
  └─ Weekly reminders

Month 18: v1 Sunset
  ├─ v1 endpoints return 410 Gone
  └─ Automatic redirects (where possible)
```

## Monitoring Deprecated Versions

We track usage of deprecated endpoints:

```
# Prometheus metric
api_deprecated_endpoint_requests_total{version="legacy", endpoint="/webhooks/voice/events"} 1234
```

## Best Practices

### For API Consumers

1. **Always specify version** in your integrations
2. **Monitor deprecation headers** in responses
3. **Test migrations** in a staging environment first
4. **Subscribe to announcements** for version updates
5. **Update documentation** when you migrate

### For Developers

1. **Never break backward compatibility** within a version
2. **Add deprecation headers** early (12 months before sunset)
3. **Provide migration guides** for all version changes
4. **Support previous version** for at least 12 months
5. **Communicate clearly** about all changes

