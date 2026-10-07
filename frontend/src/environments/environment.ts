export const environment = {
  production: false,
  azure: {
    clientId: 'aae39c25-24f8-4511-9d6f-c9321ffb3feb',
    tenantId: '03883ba6-bfd3-4f55-bc3a-0b245bf6f331',
    authority: 'https://login.microsoftonline.com/03883ba6-bfd3-4f55-bc3a-0b245bf6f331',
    redirectUri: 'http://localhost:4200/',
    protectedResourceScopes: ['api://d483f75f-ee12-4d87-b9a3-6214dd4ceb87/OT.Create']
  },
  apiBaseUrl: 'https://eeta77yl3c.execute-api.us-east-1.amazonaws.com/dev/'
};
