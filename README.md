# `squarity`: A simple chess vision trainer
> :warning: This application is still in pre-release.

## CI/CD
:warning: The below information is currently out-of-date due to reconstruction of the pipeline.
The application is hosted on Azure, in a personal Free tier account belonging to @mortenschioler. Continuous deployment is set up using GitHub Actions. The GitHub Actions workflow is defined in `.github/workflows/workflow.yml`. The GitHub-hosted runner authenticates as an Azure AD Managed Identity called a "Workload Identity" that was created manually using the commands documented in the script `/bin/create-workload-identity`. The hosted runner obtains credentials via GitHub secrets, which were added manually to the repository Actions Secrets to match the workload identity's Federated Credentials.

The pipeline runs automatically on when relevant changes are made to  `main` branch. There is currently only one environment; we test in production 🤯🔫. 
