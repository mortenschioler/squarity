// Based on the official docs: https://learn.microsoft.com/en-us/azure/app-service/quickstart-arm-template?pivots=platform-windows

@description('Web app name')
@minLength(2)
param webAppName string = uniqueString(resourceGroup().id)

@description('Location for all resources')
param location string = resourceGroup().location

@description('The SKU of App Service Plan')
param sku string = 'F1'

@description('Optional Git Repo URL')
param repoUrl string = ''

@description('Optional branch name when specifying a Git Repo URL')
param branch string = 'master'

var appServicePlanPortalName_var = 'AppServicePlan-${webAppName}'

resource appServicePlanPortalName 'Microsoft.Web/serverfarms@2020-06-01' = {
  name: appServicePlanPortalName_var
  location: location
  sku: {
    name: sku
  }
}

resource webAppName_resource 'Microsoft.Web/sites@2020-06-01' = {
  name: webAppName
  location: location
  properties: {
    reserved: false
    httpsOnly: false
    siteConfig: {}
    serverFarmId: appServicePlanPortalName.id
  }
}

resource webAppName_web 'Microsoft.Web/sites/sourcecontrols@2020-06-01' = if (!empty(repoUrl)) {
  parent: webAppName_resource
  name: 'web'
  location: location
  properties: {
    repoUrl: repoUrl
    branch: branch
    isManualIntegration: true
  }
}
