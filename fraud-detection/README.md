# fraud-detection (DEPRECATED)

> **This directory is deprecated.** The fraud detection service has been
> consolidated into [`../python-fraud-service/`](../python-fraud-service/).
>
> All new development, bug fixes, and deployments should use
> `python-fraud-service/` exclusively.

This directory contained the original minimal FastAPI fraud detection
prototype. Its logic has been migrated and expanded in the production
`python-fraud-service/` codebase, which includes:

- Multi-factor rule-based fraud detection
- ML-based anomaly detection (Isolation Forest)
- MongoDB audit logging
- Batch analysis support
- Legacy `/detect` endpoint for backward compatibility
