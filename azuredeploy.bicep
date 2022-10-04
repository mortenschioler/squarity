// Based on the official docs: https://learn.microsoft.com/en-us/azure/app-service/quickstart-arm-template?pivots=platform-windows

@description('Web app name')
@minLength(2)
param webAppName string = uniqueString(resourceGroup().id)

@description('Location for all resources')
param location string = resourceGroup().location

@description('The SKU of App Service Plan')
param sku string = 'F1'

param reserved bool = false
param httpsOnly bool = false
param arrAffinity bool = false
param http2 bool = true

var appServiceName = 'AppServicePlan-${webAppName}'

resource appService 'Microsoft.Web/serverfarms@2020-06-01' = {
  name: appServiceName
  location: location
  sku: {
    name: sku
  }
}

// Frameworkless site, which is funnily enough hosted on a Windows server. ¯\_(ツ)_/¯
resource webApp 'Microsoft.Web/sites@2020-06-01' = {
  name: webAppName
  location: location
  properties: {
    reserved: reserved
    httpsOnly: httpsOnly
    clientAffinityEnabled: arrAffinity
    siteConfig: {
      http20Enabled: http2
    }
    serverFarmId: appService.id
  }
}
