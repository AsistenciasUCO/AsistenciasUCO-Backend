function fn() {
  var env = karate.env; // obtener propiedad del sistema karate.env si existe
  karate.log('Karate environment running:', env);

  if (!env) {
    env = 'local';
  }

  var port = karate.properties['server.port'] || '8080';
  var config = {
    env: env,
    baseUrl: 'http://localhost:' + port
  };

  karate.configure('connectTimeout', 5000);
  karate.configure('readTimeout', 10000);

  return config;
}
