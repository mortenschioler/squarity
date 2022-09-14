# `squarity`: A simple chess vision trainer
> :warning: This application is still in pre-release.

## CI/CD
The application is hosted on Azure, in a personal Free tier account belonging to @mortenschioler. Continuous deployment is set up using GitHub Actions. The GitHub Actions workflow is defined in `.github/workflows/workflow.yml`. The GitHub-hosted runner authenticates as an Azure AD Managed Identity called a "Workload Identity" that was created manually using the commands documented in the script `/bin/create-workload-identity`. The hosted runner obtains credentials via GitHub secrets, which were added manually to the repository Actions Secrets to match the workload identity's Federated Credentials.