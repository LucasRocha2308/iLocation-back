package br.com.verbososcorp.ilocation.Filters;

import br.com.verbososcorp.ilocation.DTO.DeliveryPersonDTO;
import br.com.verbososcorp.ilocation.exceptions.ErrorMessage;
import br.com.verbososcorp.ilocation.exceptions.customExceptions.BadRequestException;
import br.com.verbososcorp.ilocation.models.DeliveryPerson;
import br.com.verbososcorp.ilocation.services.interfaces.DeliveryPersonService;
import br.com.verbososcorp.ilocation.util.Project;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static br.com.verbososcorp.ilocation.util.Project.getAlgorithm;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;


public class CustomAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    public static final int MILI_SECS = 1;
    public static final int SECS = MILI_SECS * 1000;
    public static final int MINUTES = SECS * 60;
    public static final int HOURS = MINUTES * 60;

    public static final String HEADER_ATRIBUTO = "Authorization";
    public static final String ATRIBUTO_PREFIXO = "Bearer ";

    public static final String ISSUER = "Admin";
    public static final String TOKEN_SENHA = "ULTRASECRETAPRANAOROUPAMEUDADOS=(";

    private final AuthenticationManager authenticationManager;

    private DeliveryPersonDTO userDTO;

    private final DeliveryPersonService deliveryPersonService;


    public CustomAuthenticationFilter(AuthenticationManager authenticationManager, DeliveryPersonService deliveryPersonService) {
        this.authenticationManager = authenticationManager;
        this.deliveryPersonService = deliveryPersonService;
        this.setFilterProcessesUrl(Project.BASE_URL + "/login");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        try{
            DeliveryPerson user = new ObjectMapper().readValue(request.getInputStream(), DeliveryPerson.class);
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword());

            this.userDTO = deliveryPersonService.getByEmailForAuth(user.getEmail());

            return authenticationManager.authenticate(authenticationToken);

        } catch (IOException e) {
            throw new BadRequestException("Verifique seus dados e tente novamente!");
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) throws IOException, ServletException {
        User user = (User) authentication.getPrincipal();

        Algorithm algorithm = getAlgorithm(TOKEN_SENHA);

        String access_token = JWT.create()
                .withSubject(user.getUsername())
                .withClaim("id", userDTO.getId())
                .withClaim( "name", userDTO.getName())
                .withClaim("phone", userDTO.getPhone())
                .withExpiresAt(new Date(System.currentTimeMillis() + HOURS*2))
                .withIssuer(ISSUER)
                .sign(algorithm);

        String refresh_token = JWT.create()

                .withSubject(user.getUsername())
                .withExpiresAt(new Date(System.currentTimeMillis()+ HOURS*2))
                .withIssuer(ISSUER)
                .sign(algorithm);


        Map<String, String> tokens = new HashMap<>();
        tokens.put("access_token", ATRIBUTO_PREFIXO + access_token);
        tokens.put("refresh_token", ATRIBUTO_PREFIXO + refresh_token);

        response.setContentType(APPLICATION_JSON_VALUE);

        String responseObject = new ObjectMapper().writeValueAsString(tokens);
        response.getWriter().write(responseObject);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        ErrorMessage errorMessage = new ErrorMessage(401, new Date(), "Login ou senha invalido.", request.getServletPath());
        response.setContentType(APPLICATION_JSON_VALUE);
        response.setStatus(401);
        new ObjectMapper().writeValue(response.getOutputStream(), errorMessage);
    }
}
