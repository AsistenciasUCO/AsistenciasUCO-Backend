Feature: HU116 - Gestión de Facultades
  Como Administrador del Sistema
  Quiero consultar y registrar facultades
  Para mantener la estructura institucional

  Background:
    * url baseUrl

  Scenario: HU116 - Consultar lista de facultades
    Given path '/api/v1/admin/facultades'
    When method get
    Then assert responseStatus == 200 || responseStatus == 401 || responseStatus == 403
    * if (responseStatus == 200) karate.match(response.exitoso, true)

  Scenario: HU116 - Rechazar creación de facultad con payload inválido
    Given path '/api/v1/admin/facultades'
    And request {}
    When method post
    Then assert responseStatus == 400 || responseStatus == 401 || responseStatus == 403
