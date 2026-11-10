// Configuración de PRODUCCIÓN (la que se usa al desplegar en la nube).
// Para correr en local se usa environment.development.ts (ese NO se toca).
export const environment = {
  production: true,
  azure: {
    clientId: 'aae39c25-24f8-4511-9d6f-c9321ffb3feb',
    tenantId: '03883ba6-bfd3-4f55-bc3a-0b245bf6f331',
    authority: 'https://login.microsoftonline.com/03883ba6-bfd3-4f55-bc3a-0b245bf6f331',
    // Toma automáticamente la URL donde está publicada la app
    redirectUri: window.location.origin + '/',
    protectedResourceScopes: ['api://d483f75f-ee12-4d87-b9a3-6214dd4ceb87/OT.Create']
  },
  // En la nube, Nginx redirige /order/... al order-service
  orderServiceUrl: '/order',
  apiBaseUrl: 'https://eeta77yl3c.execute-api.us-east-1.amazonaws.com/dev/'
};