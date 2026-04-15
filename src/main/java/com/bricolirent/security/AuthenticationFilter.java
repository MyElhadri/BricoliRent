package com.bricolirent.security;

import com.bricolirent.service.impl.AuthServiceImpl;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Filtre d'authentification protégeant toutes les pages sous {@code /app/*}.
 *
 * <p>Comportement :</p>
 * <ul>
 *   <li>Laisse passer les ressources JSF statiques ({@code /jakarta.faces.resource/})</li>
 *   <li>Laisse passer {@code /index.xhtml} et {@code /login.xhtml}</li>
 *   <li>Bloque toute requête vers {@code /app/*} si la session ne contient pas
 *       l'attribut {@code SESSION_USER} (utilisateur connecté)</li>
 *   <li>Redirige vers {@code /login.xhtml} en cas d'accès non autorisé</li>
 * </ul>
 *
 * <p>Le filtre est automatiquement détecté par le conteneur Jakarta EE
 * grâce à l'annotation {@code @WebFilter}.</p>
 */
@WebFilter(urlPatterns = {"/app/*"}, asyncSupported = true)
public class AuthenticationFilter implements Filter {

    private static final Logger LOGGER = Logger.getLogger(AuthenticationFilter.class.getName());

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        LOGGER.info("[AuthFilter] Filtre d'authentification initialisé.");
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  request  = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String requestURI = request.getRequestURI();

        // ----------------------------------------------------------------
        // 1) Toujours laisser passer les ressources statiques JSF/Jakarta
        //    (CSS, JS, images passant par FacesServlet sous ce chemin)
        // ----------------------------------------------------------------
        if (requestURI.contains("/jakarta.faces.resource/")) {
            chain.doFilter(req, res);
            return;
        }

        // ----------------------------------------------------------------
        // 2) Vérifier la présence de l'utilisateur en session
        // ----------------------------------------------------------------
        HttpSession session = request.getSession(false);
        boolean loggedIn = (session != null)
                && (session.getAttribute(AuthServiceImpl.SESSION_USER_KEY) != null);

        if (loggedIn) {
            String role = (String) session.getAttribute(AuthServiceImpl.SESSION_ROLE_KEY);
            
            // ROLE-BASED ACCESS CONTROL
            if (requestURI.contains("/app/admin/") && !"ADMIN".equals(role)) {
                LOGGER.warning("[AuthFilter] Accès refusé (Admin requis) : " + requestURI);
                response.sendRedirect(request.getContextPath() + "/app/dashboard.xhtml");
                return;
            }
            if (requestURI.contains("/app/agent/") && !"AGENT".equals(role)) {
                LOGGER.warning("[AuthFilter] Accès refusé (Agent requis) : " + requestURI);
                response.sendRedirect(request.getContextPath() + "/app/dashboard.xhtml");
                return;
            }
            if (requestURI.contains("/app/client/") && !"CLIENT".equals(role)) {
                LOGGER.warning("[AuthFilter] Accès refusé (Client requis) : " + requestURI);
                response.sendRedirect(request.getContextPath() + "/app/dashboard.xhtml");
                return;
            }

            // Utilisateur connecté → continuer normalement
            chain.doFilter(req, res);
        } else {
            // Non connecté → rediriger vers login.xhtml
            LOGGER.info("[AuthFilter] Accès refusé (" + requestURI + ") → redirection vers login.xhtml");
            String loginPage = request.getContextPath() + "/login.xhtml";
            response.sendRedirect(loginPage);
        }
    }

    @Override
    public void destroy() {
        LOGGER.info("[AuthFilter] Filtre d'authentification détruit.");
    }
}
