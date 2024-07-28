# spring-security
Spring Security is a powerful and highly customizable security framework that provides 
authentication, authorization, and other security features for your applications.

### Part 1: Application config

1. Spring Initializer : https://start.spring.io/
2. Add dependency : `lombok`, `spring-boot-starter-web`, `spring-boot-starter-security`, `h2-database`
   <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-devtools</artifactId>
      <scope>runtime</scope>
      <optional>true</optional>
    </dependency>
    <dependency>
      <groupId>com.mysql</groupId>
      <artifactId>mysql-connector-j</artifactId>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>org.projectlombok</groupId>
      <artifactId>lombok</artifactId>
      <optional>true</optional>
    </dependency>
   ```

### Part 2: Adding User 

1. Create a `UserEntity`, which holds User Data in `entity` package
    ```java
    @Entity
    @Table(name = "users")
    @Data
    @NoArgsConstructor
    public class UserEntity {
    
        @Id
        @GeneratedValue
        private Long id;
        private String username;
        private String password;
        private String roles;
    }

   ```

2. Create a `ORM-Mapping` in `UserRepo` file. 

    ```java

    @Repository
    public interface UserRepo  extends JpaRepository<UserEntity,Long> {
    }

   ```

3. Add `SecurityConfig`, which allows `h2-console` to be accessed without password, and also restrict other `api` calls without password

   ```java
   @Configuration
   @EnableWebSecurity
   public class SecurityConfig {
   
       @Bean
       @Order(1)
       public SecurityFilterChain h2ConsoleSecurityFilterChainConfig(HttpSecurity httpSecurity) throws Exception{
           return httpSecurity
                   .securityMatcher(new AntPathRequestMatcher(("/h2-console/**")))
                   .authorizeHttpRequests(auth->auth.anyRequest().permitAll())
                   .csrf(csrf -> csrf.ignoringRequestMatchers(AntPathRequestMatcher.antMatcher("/h2-console/**")))
                   .headers(headers -> headers.frameOptions(withDefaults()).disable())
                   .build();
       }
   
       @Bean
       @Order(2)
       public SecurityFilterChain apiSecurityFilterChain(HttpSecurity httpSecurity) throws Exception {
           return httpSecurity
                   .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                   .formLogin(withDefaults())
                   .build();
       }
   
       @Bean
       PasswordEncoder passwordEncoder(){
           return new BCryptPasswordEncoder();
       }
   
   }

   ```
4. Let's add user in our table using `command-line-runner`
    ```java
    @SpringBootApplication
    public class SpringSecurityApplication {
    
        public static void main(String[] args) {
            SpringApplication.run(SpringSecurityApplication.class, args);
        }
    
        //Command line runner: After the application context, but before the application starts
        @Bean
        CommandLineRunner commandLineRunner(UserRepo userRepo, PasswordEncoder passwordEncoder){
            return args -> {
                UserEntity manager = new UserEntity();
                manager.setUsername("manager");
                manager.setPassword(passwordEncoder.encode("password"));
                manager.setRoles("ROLE_MANAGER");

                UserEntity admin = new UserEntity();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("password"));
                admin.setRoles("ROLE_MANAGER,ROLE_ADMIN");
                
                userRepo.saveAll(List.of(manager,admin));
            };
        }
    }

   ```
5. Let's start the application and check these url `http://localhost:8080/h2-console`, and you can see that the user `atquil` is present in the database


### Part 3: Configuring `SecurityConfig`, to use this user to access the `api`

1. Create a file `UserSecurityConfig` which will implement `UserDetails`, for `Authentication`, using the `UserEntity` object. 

   **UserDetails simply store user info which is later encapsulated into Authentication object.**
   ```java
   @RequiredArgsConstructor
   public class UserSecurityConfig implements UserDetails {
   
   
       private final UserEntity userEntity;
       @Override
       public String getUsername() {
           return userEntity.getUsername();
       }
   
       @Override
       public String getPassword() {
           return userEntity.getPassword();
       }
       @Override
       public Collection<? extends GrantedAuthority> getAuthorities() {
           return Arrays
                   .stream(userEntity
                           .getRoles()
                           .split(","))
                   .map(SimpleGrantedAuthority::new)
                   .toList();
       }
   
       @Override
       public boolean isAccountNonExpired() {
           return true;
       }
   
       @Override
       public boolean isAccountNonLocked() {
           return true;
       }
   
       @Override
       public boolean isCredentialsNonExpired() {
           return true;
       }
   
       @Override
       public boolean isEnabled() {
           return true;
       }
   }
   ```
   
2. Now Create a `JPAUserDetailsManagerConfig` file, which will use `UserSecurityConfig`, and `UserEntity` to find the user and map it for `Authentication`

   ```java
   @Service
   @RequiredArgsConstructor
   public class JPAUserDetailsManagerConfig implements UserDetailsService {
   
       private final UserRepo userRepo;
       @Override
       public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
           return userRepo
                   .findByUsername(username) // Create the method like this Optional<UserEntity> findByUsername(String username);
                   .map(UserSecurityConfig::new)
                   .orElseThrow(()-> new UsernameNotFoundException("User: "+username+" does not exist"));
       }
   }
   ```
   
   - You also need to create a method called `findByUserName` in `UserRepo`
   ```java
   @Repository
   public interface UserRepo  extends JpaRepository<UserEntity,Long> {
   Optional<UserEntity> findByUsername(String username);
   }

   ```
   
3. Finally, point the `Authentication` to use `JPAUserDetailsManagerConfig`, instead of default one

   ```java
   @Configuration
   @EnableWebSecurity
   @RequiredArgsConstructor
   public class SecurityConfig {
   
       private final JPAUserDetailsManagerConfig jpaUserDetailsManagerConfig;
      //...
   
       @Bean
       @Order(2)
       public SecurityFilterChain apiSecurityFilterChain(HttpSecurity httpSecurity) throws Exception {
           return httpSecurity
                    .securityMatcher(new AntPathRequestMatcher("/api/**"))
                    .csrf(AbstractHttpConfigurer::disable) //CRSF protection is a crucial security measure to prevent Cross-Site Forgery attacks. Hence, it’s advisable to include the CRSF token in the request header of state-changing operations.
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .userDetailsService(jpaUserDetailsManagerConfig)
                    .formLogin(withDefaults())
                    .httpBasic(withDefaults())
                    .build();
       }
   
      //...
   
   }

   ```
### Part 4: RoleBasedAuth with Endpoint

1. Create a `UserController` and add 2 endpoints. 
   ```java
   @RestController
   @RequestMapping("/api")
   @RequiredArgsConstructor
   public class UserController {
   
       private final UserRepo userRepo;
   
       @GetMapping("/anyone")
       public ResponseEntity<?> getTestAPI1(){
           return ResponseEntity.ok("Response");
       }
   
   
       @PreAuthorize("hasRole('ROLE_MANAGER')")
       @GetMapping("/manager")
       public ResponseEntity<?> getTestAPI2(Principal principal){
   
           return ResponseEntity.ok(principal.getName()+" : is accessing manager api. All data from backend"+ userRepo.findAll());
       }
   
   
      
       @PreAuthorize("hasAnyRole('ROLE_MANAGER','ROLE_ADMIN')")
       @GetMapping("/admin")
       public ResponseEntity<?> getTestAPI3(Principal principal){
           return ResponseEntity.ok(principal.getName()+" : is accessing admin api. All data from backend"+ userRepo.findAll());
       }
   }

   ```
2. Also add `@EnableMethodSecurity` in securityConfig file

   ```java
      @Configuration
      @EnableWebSecurity
      @RequiredArgsConstructor
      @EnableMethodSecurity
      public class SecurityConfig {
      //...
      }
   ```
   
3. Test with the url : Let's create user Manager and Admin

| API                                 | Access |
|-------------------------------------|--------|
| `http://localhost:8080/api/anyone`  | Yes    |
| `http://localhost:8080/api/manager` | Yes    |
| `http://localhost:8080/api/admin`   | No     |

