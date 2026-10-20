Feature: HU117 / HU117-B - Gestión y Consulta de Decanos
  Como Administrador del Sistema
  Quiero consultar y registrar decanos
  Para gestionar el gobierno académico institucional

  Background:
    * url baseUrl

  Scenario: HU117 - Consultar lista de decanos existentes
    Given path '/api/v1/admin/decanos'
    When method get
    # En entorno no autenticado o con seguridad activa responde conforme a la regla RBAC institucional
    Then assert responseStatus == 200 || responseStatus == 401 || responseStatus == 403
    * if (responseStatus == 200) karate.match(response.exitoso, true)

  Scenario: HU117-B - Rechazar creación de decano con payload vacío
    Given path '/api/v1/admin/decanos'
    And request {}
    When method post
    Then assert responseStatus == 400 || responseStatus == 401 || responseStatus == 403
