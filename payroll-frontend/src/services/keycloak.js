import Keycloak from "keycloak-js";

const keycloak = new Keycloak({
  url: "http://localhost:9090",
  realm: "sisgr-realm",
  clientId: "sisgr-frontend",
});

export default keycloak;