package com.matrix.security.config;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService;

@Configuration
@EnableAuthorizationServer
public class AuthorizationServerConfig extends AuthorizationServerConfigurerAdapter {
	@Autowired
	AuthenticationManager authenticationManager;

	@Autowired
	RedisConnectionFactory redisConnectionFactory;

	@Autowired(required = true)
	@Qualifier("myUserDetailsService")
	public UserDetailsService userDetailsService;

	@Resource
	private DataSource dataSource;

	@Override
	public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
//		configureWithMemory(clients);
		configureWithJdbc(clients);
	}

	private void configureWithMemory(ClientDetailsServiceConfigurer clients) throws Exception {
		String finalSecret = "{bcrypt}" + new BCryptPasswordEncoder().encode("123456");
//		String finalSecret = "{bcrypt}" + passwordEncoder.encode("123456");

		// 配置两个客户端，一个用于password认证一个用于client认证
		clients.inMemory().withClient("client_1").resourceIds(Utils.RESOURCEIDS.ORDER)
				.authorizedGrantTypes("client_credentials", "refresh_token", "authorization_code").scopes("select")
				.authorities("oauth2").secret(finalSecret).and().withClient("client_2")
				.resourceIds(Utils.RESOURCEIDS.ORDER)
				.authorizedGrantTypes("password", "refresh_token", "authorization_code").scopes("server")
				.authorities("oauth2").secret(finalSecret).and().withClient("client_3")
				.resourceIds(Utils.RESOURCEIDS.ORDER).secret(finalSecret).authorizedGrantTypes("authorization_code")
				.scopes("app").redirectUris("http://www.baidu.com").and().withClient("client_4")
				.resourceIds(Utils.RESOURCEIDS.ORDER).secret(finalSecret).authorizedGrantTypes("implicit")
				.scopes("test1").redirectUris("http://www.baidu.com");
//		String[] permissions = "ADMIN2,ROLE_ADMIN".split(",");
//		List<GrantedAuthority> authorities = new ArrayList<>();
//		for (String permission : permissions) {
//		    authorities.add(new SimpleGrantedAuthority(permission));
//		}
//		userDetails.setAuthorities(authorities);
//		@PreAuthorize("hasRole('ADMIN')")                //允许
//		@PreAuthorize("hasRole('ROLE_ADMIN')")           //允许
//		@PreAuthorize("hasRole('ADMIN2')")               //不允许
//		@PreAuthorize("hasRole('ROLE_ADMIN2')")          //不允许
//		@PreAuthorize("hasAuthority('ADMIN2')")          //允许
//		@PreAuthorize("hasAuthority('ROLE_ADMIN2')")     //不允许
//		@PreAuthorize("hasAuthority('ADMIN')")           //不允许
//		@PreAuthorize("hasAuthority('ROLE_ADMIN')")      //允许
//		https://blog.csdn.net/qq_26878363/article/details/103632459?utm_medium=distribute.pc_relevant_t0.none-task-blog-BlogCommendFromBaidu-1.control&depth_1-utm_source=distribute.pc_relevant_t0.none-task-blog-BlogCommendFromBaidu-1.control
	}

	private void configureWithJdbc(ClientDetailsServiceConfigurer clients) throws Exception {

		// 默认值InMemoryTokenStore对于单个服务器是完全正常的（即，在发生故障的情况下，低流量和热备份备份服务器）。大多数项目可以从这里开始，也可以在开发模式下运行，以便轻松启动没有依赖关系的服务器。
		// 这JdbcTokenStore是同一件事的JDBC版本，它将令牌数据存储在关系数据库中。如果您可以在服务器之间共享数据库，则可以使用JDBC版本，如果只有一个，则扩展同一服务器的实例，或者如果有多个组件，则授权和资源服务器。要使用JdbcTokenStore你需要“spring-jdbc”的类路径。
		// 这个地方指的是从jdbc查出数据来存储
		clients.withClientDetails(clientDetails());
	}

	@Bean
	public ClientDetailsService clientDetails() {
		return new JdbcClientDetailsService(dataSource);
	}

	@Override
	public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
		endpoints.tokenStore(new MatrixRedisTokenStore(redisConnectionFactory)).authenticationManager(authenticationManager)
				.allowedTokenEndpointRequestMethods(HttpMethod.GET, HttpMethod.POST)
				.userDetailsService(userDetailsService);

	}

	@Override
	public void configure(AuthorizationServerSecurityConfigurer security) throws Exception {
		// 允许表单认证
		security.allowFormAuthenticationForClients();
		// 开启/oauth/token_key验证端口无权限访问
		security.tokenKeyAccess("permitAll()");
		// 开启/oauth/check_token验证端口认证权限访问
		security.checkTokenAccess("isAuthenticated()");
	}
}
