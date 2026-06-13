<div align="center">

# 🏢 ERP Tony — Backend

### API REST empresarial construida con Spring Boot 3 y Java 21

Sistema de gestión empresarial orientado a la administración de inventario, clientes, usuarios y ventas. Implementa autenticación JWT, control de acceso por roles y generación de informes en PDF y CSV.

<br>

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.x-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-Authentication-black?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)

<br>

**🔐 Seguridad JWT • 📦 Inventario • 👥 Clientes • 🧾 Ventas • 📊 Informes**

</div>

---

# 📖 Tabla de Contenidos

- [Descripción](#-descripción)
- [Características](#-características)
- [Tecnologías](#-tecnologías)
- [Arquitectura](#-arquitectura)
- [Instalación](#-instalación)
- [Configuración](#-configuración)
- [Endpoints](#-endpoints-principales)
- [Seguridad](#-seguridad)
- [Exportación de Informes](#-exportación-de-informes)
- [Decisiones de Diseño](#-decisiones-de-diseño)
- [Roadmap](#-roadmap)
- [Licencia](#-licencia)

---

# 🎯 Descripción

ERP Tony Backend constituye la capa de negocio y persistencia de datos del ecosistema ERP Tony.

La aplicación expone una API REST stateless diseñada para ser consumida por un cliente JavaFX y proporciona funcionalidades de gestión comercial mediante una arquitectura basada en Spring Boot.

## Funcionalidades principales

- 🔐 Autenticación mediante JWT
- 👤 Gestión de usuarios y roles
- 📦 Gestión de inventario y categorías
- 👥 Administración de clientes
- 🧾 Registro y control de ventas
- 📊 Generación de informes PDF y CSV
- 🛡️ Contraseñas cifradas mediante BCrypt

---

# ✨ Características

## 🔐 Seguridad

- Autenticación basada en JWT
- Control de acceso por roles (`ROLE_ADMIN` y `ROLE_EMPLOYEE`)
- Contraseñas protegidas con BCrypt
- Arquitectura Stateless

## 📦 Inventario

- CRUD completo de productos
- Gestión de categorías
- Búsqueda por nombre
- Borrado lógico

## 👥 Clientes

- Alta, modificación y consulta
- Conservación del historial comercial
- Búsqueda por nombre o NIF

## 🧾 Ventas

- Registro de ventas
- Actualización automática de stock
- Cancelación con reposición de inventario
- Historial completo de operaciones

## 📊 Informes

- Exportación PDF
- Exportación CSV compatible con Excel
- Generación de facturas

---

# 🛠 Tecnologías

| Componente | Tecnología | Versión |
|------------|------------|----------|
| Lenguaje | Java | 21 |
| Framework | Spring Boot | 3.5 |
| Seguridad | Spring Security | 6.x |
| Persistencia | Spring Data JPA | 3.x |
| ORM | Hibernate | 6.x |
| Base de datos | MySQL | 8.x |
| Autenticación | JWT | HS256 |
| Hashing | BCrypt | — |
| PDF | OpenPDF | 1.3.30 |
| Excel / CSV | Apache POI | 5.2.5 |
| Validación | Jakarta Validation | — |
| Utilidades | Lombok | — |

---

# 🏛 Arquitectura

El proyecto sigue una arquitectura en capas para mantener una clara separación de responsabilidades.

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
MySQL
```

---

## 📂 Estructura del Proyecto

```text
src
└── main
    ├── java
    │   └── com.tony.erp
    │
    │       ├── config
    │       │   ├── JwtFilter.java
    │       │   ├── JwtProvider.java
    │       │   └── SecurityConfig.java
    │       │
    │       ├── controller
    │       │   ├── CategoryController.java
    │       │   ├── ClientController.java
    │       │   ├── ProductController.java
    │       │   ├── SaleController.java
    │       │   └── UserController.java
    │       │
    │       ├── model
    │       │   ├── Category.java
    │       │   ├── Client.java
    │       │   ├── Product.java
    │       │   ├── Role.java
    │       │   ├── Sale.java
    │       │   ├── SaleItem.java
    │       │   └── User.java
    │       │
    │       ├── repository
    │       │   ├── CategoryRepository.java
    │       │   ├── ClientRepository.java
    │       │   ├── ProductRepository.java
    │       │   ├── RoleRepository.java
    │       │   ├── SaleRepository.java
    │       │   └── UserRepository.java
    │       │
    │       ├── service
    │       │   ├── CategoryService.java
    │       │   ├── ClientService.java
    │       │   ├── CustomUserDetailsService.java
    │       │   ├── ProductService.java
    │       │   ├── SaleService.java
    │       │   └── UserService.java
    │       │
    │       └── ErpApplication.java
    │
    └── resources
        └── application.properties
```

---

## 🔐 Configuración de Seguridad

| Clase | Responsabilidad |
|---------|---------|
| `JwtFilter` | Intercepta y valida los tokens JWT |
| `JwtProvider` | Genera, valida y procesa tokens JWT |
| `SecurityConfig` | Configura Spring Security y las reglas de acceso |

---

## 🌐 Controladores REST

| Controlador | Función |
|------------|----------|
| `UserController` | Usuarios y autenticación |
| `ProductController` | Gestión de productos |
| `CategoryController` | Gestión de categorías |
| `ClientController` | Gestión de clientes |
| `SaleController` | Gestión de ventas |

---

## ⚙️ Servicios

Implementan la lógica de negocio de la aplicación.

- `UserService`
- `ProductService`
- `CategoryService`
- `ClientService`
- `SaleService`
- `CustomUserDetailsService`

---

## 🗄 Repositorios

Acceso a datos mediante Spring Data JPA.

- `UserRepository`
- `RoleRepository`
- `ProductRepository`
- `CategoryRepository`
- `ClientRepository`
- `SaleRepository`

---

## 📦 Entidades

Representan el modelo de dominio de la aplicación.

- `User`
- `Role`
- `Product`
- `Category`
- `Client`
- `Sale`
- `SaleItem`

---

# 🚀 Instalación

## Requisitos

- Java 21
- Maven 3.8+
- MySQL 8+

## Clonar el repositorio

```bash
git clone https://github.com/tu-usuario/erp-backend.git

cd erp-backend
```

## Crear la base de datos

```sql
CREATE DATABASE erp_db;
```

## Ejecutar la aplicación

```bash
mvn clean install

mvn spring-boot:run
```

Servidor disponible en:

```text
http://localhost:8080
```

---

# ⚙️ Configuración

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/erp_db
spring.datasource.username=root
spring.datasource.password=tu_password

spring.jpa.hibernate.ddl-auto=update

server.port=8080
```

---

# 📡 Endpoints Principales

## 🔓 Autenticación

| Método | Endpoint |
|----------|-----------|
| POST | `/api/usuarios/login` |
| POST | `/api/usuarios/registro` |

## 📦 Productos

| Método | Endpoint |
|----------|-----------|
| GET | `/api/products` |
| GET | `/api/products/{id}` |
| POST | `/api/products` |
| PUT | `/api/products/{id}` |
| DELETE | `/api/products/{id}` |

## 👥 Clientes

| Método | Endpoint |
|----------|-----------|
| GET | `/api/clients` |
| POST | `/api/clients` |
| PUT | `/api/clients/{id}` |
| DELETE | `/api/clients/{id}` |

## 🧾 Ventas

| Método | Endpoint |
|----------|-----------|
| GET | `/api/sales` |
| POST | `/api/sales` |
| PUT | `/api/sales/{id}/cancel` |

---

# 🔐 Seguridad

La aplicación utiliza Spring Security junto con JWT para implementar autenticación y autorización.

## Flujo de autenticación

```text
Cliente
   │
   ▼
Login
   │
   ▼
JWT Token
   │
   ▼
Authorization: Bearer <token>
   │
   ▼
JwtFilter
   │
   ▼
SecurityConfig
   │
   ▼
Endpoint protegido
```

## Roles

| Recurso | ADMIN | EMPLOYEE |
|----------|:---:|:---:|
| Productos | ✅ | ✅ |
| Clientes | ✅ | ✅ |
| Ventas | ✅ | ✅ |
| Gestión de usuarios | ✅ | ❌ |

## Contraseñas

Las credenciales se almacenan utilizando BCrypt.

```java
passwordEncoder.encode(password);
```

---

# 📊 Exportación de Informes

| Recurso | PDF | CSV |
|----------|------|------|
| Productos | ✅ | ✅ |
| Facturas | ✅ | ✅ |

Los informes son generados dinámicamente desde el backend utilizando OpenPDF y Apache POI.

---

# 🧠 Decisiones de Diseño

## Borrado lógico

Los productos y clientes no se eliminan físicamente de la base de datos.

Esto permite preservar la integridad histórica de las ventas realizadas.

## Arquitectura Stateless

El servidor no almacena sesiones.

Toda la autenticación se realiza mediante JWT enviados en cada petición.

## Separación de responsabilidades

Cada capa posee una responsabilidad única:

- Controller → Exposición HTTP
- Service → Lógica de negocio
- Repository → Persistencia
- Model → Dominio

## BCrypt para contraseñas

Las contraseñas nunca se almacenan en texto plano.

Se utiliza hashing adaptativo mediante BCrypt para aumentar la seguridad.

---

# 🗺 Roadmap

- [x] Autenticación JWT
- [x] Gestión de productos
- [x] Gestión de clientes
- [x] Gestión de ventas
- [x] Exportación PDF y CSV
- [ ] DTOs
- [ ] Swagger / OpenAPI
- [ ] Refresh Tokens
- [ ] Flyway
- [ ] Docker
- [ ] Tests de integración
- [ ] GitHub Actions CI/CD

---

# 📄 Licencia

Este proyecto está licenciado bajo la licencia **MIT**.

Puedes utilizarlo, modificarlo y distribuirlo libremente respetando los términos establecidos en el archivo [`LICENSE`](LICENSE).

---

<div align="center">

### ⭐ Gracias por visitar ERP Tony

Una API REST desarrollada con enfoque en:

🔐 Seguridad · 📦 Inventario · 👥 Clientes · 🧾 Ventas · 📊 Informes

Construido con **Java 21**, **Spring Boot 3**, **Spring Security** y **MySQL**.

Si te ha parecido interesante, considera apoyar el proyecto dejando una estrella en GitHub.

</div>
