param location string = resourceGroup().location

module appService 'modules/appService.bicep' = {
  name: 'AppService'
  params: {
    appServiceAppName: 'squarity'
    appServicePlanName:  'squarity-app-plan'
    location: location
    appServicePlanSkuName: 'F1'
  }
}

output appServiceAppHostName string = appService.outputs.appHostName
