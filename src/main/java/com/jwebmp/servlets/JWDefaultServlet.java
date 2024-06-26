/*
 * Copyright (C) 2017 GedMarc
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.jwebmp.servlets;

import com.guicedee.client.*;

import com.guicedee.guicedservlets.GuicedServletKeys;
import com.jwebmp.core.base.interfaces.IComponentHierarchyBase;
import com.jwebmp.core.services.IEventTypes;
import com.jwebmp.core.services.IPage;
import com.jwebmp.interception.services.StaticStrings;
import com.jwebmp.core.base.ajax.AjaxCall;
import com.jwebmp.core.base.ajax.AjaxResponse;
import com.jwebmp.core.exceptions.InvalidRequestException;
import com.jwebmp.core.exceptions.MissingComponentException;
import com.jwebmp.core.services.IErrorPage;
import com.jwebmp.interception.services.SiteCallIntercepter;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.extern.java.Log;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.logging.Level;

import static com.guicedee.client.IGuiceContext.get;
import static com.jwebmp.interception.services.JWebMPInterceptionBinder.SiteCallInterceptorKey;

/**
 * Provides default methods for authentication authorization etc
 *
 * @author GedMarc
 * @version 1.0
 * @since Nov 14, 2016
 */
@Log
@SuppressWarnings("unused")
public abstract class JWDefaultServlet
        extends HttpServlet
{
    /**
     * Field allowOrigin
     */
    private static String allowOrigin = "*";

    /**
     * Construct a new default servlett
     */
    public JWDefaultServlet()
    {
        //Nothing needed
    }

    /**
     * Sets the stream allow origins
     *
     * @return The current origins allowed
     */
    public static String getAllowOrigin()
    {
        return JWDefaultServlet.allowOrigin;
    }

    /**
     * Sets the streams allow origins
     *
     * @param allowOrigin The allowed origins, default *
     */
    public static void setAllowOrigin(@NotNull String allowOrigin)
    {
        JWDefaultServlet.allowOrigin = allowOrigin;
    }

    /**
     * Validates the given call for the servlet
     *
     * @param ajaxCall optional parameter to validate on more fields
     * @return If this call is valid
     */
    @SuppressWarnings({"WeakerAccess",
            "UnusedReturnValue"})
    public boolean validateCall(AjaxCall<?> ajaxCall) throws InvalidRequestException
    {
        HttpServletRequest request = get(GuicedServletKeys.getHttpServletRequestKey());
        if (ajaxCall.getComponentId() == null)
        {
            JWDefaultServlet.log.log(Level.SEVERE, "[SessionID]-[{0}];[Security]-[Component ID Not Found]", request.getSession()
                                                                                                                   .getId());
            throw new InvalidRequestException("There is no Component ID in this call.");
        }

        String componentId = ajaxCall.getComponentId();
        if (componentId.isEmpty())
        {
            JWDefaultServlet.log.log(Level.FINER, "[SessionID]-[{0}];[Security]-[Component ID Incorrect]", request.getSession()
                                                                                                                  .getId());
        }
        return true;
    }

    /**
     * Validates if the page is found and correct
     *
     * @return If the page can be retrieved
     * @throws com.jwebmp.core.exceptions.MissingComponentException if page returned is null
     */
    @SuppressWarnings({"WeakerAccess",
            "UnusedReturnValue"})
    public boolean validatePage() throws MissingComponentException
    {
        IPage<?> page = get(IPage.class);
        if (page == null)
        {
            throw new MissingComponentException(
                    "Page has not been bound yet. Please use a binder to map Page to the required page object. Also consider using a @Provides method to apply custom logic. See https://github.com/google/guice/wiki/ProvidesMethods ");
        }
        return true;
    }

    /**
     * Validates the request if it from a legitimate source
     *
     * @param ajaxCall The incoming call object
     * @return if the request was validated
     * @throws InvalidRequestException If the request is invalid
     */
    @SuppressWarnings({"WeakerAccess",
            "UnusedReturnValue"})
    public boolean validateRequest(AjaxCall<?> ajaxCall) throws InvalidRequestException
    {
        HttpServletRequest request = get(GuicedServletKeys.getHttpServletRequestKey());
        Date datetime = ajaxCall.getDatetime();
        if (datetime == null)
        {
            JWDefaultServlet.log.log(Level.SEVERE, "[SessionID]-[{0}];[Security]-[Date Time Incorrect]", request.getSession()
                                                                                                                .getId());
            throw new InvalidRequestException("Invalid Date Time Value");
        }
        IEventTypes eventType = ajaxCall.getEventType();
        if (eventType == null)
        {
            JWDefaultServlet.log.log(Level.SEVERE, "[SessionID]-[{0}];[Security]-[Event Type Incorrect]", request.getSession()
                                                                                                                 .getId());
            throw new InvalidRequestException("Invalid Event Type");
        }
        for (SiteCallIntercepter<?> siteCallIntercepter : get(SiteCallInterceptorKey))
        {
            siteCallIntercepter.intercept(ajaxCall, IGuiceContext.get(AjaxResponse.class));
        }

        return true;
    }

    /**
     * Generates the Page HTML
     *
     * @return The page html
     */
    @SuppressWarnings("WeakerAccess")
    protected StringBuilder getPageHTML()
    {
        StringBuilder html;
        try
        {
            html = new StringBuilder(getPageFromGuice().toString(true));
        }
        catch (Throwable T)
        {
            return new StringBuilder(ExceptionUtils.getStackTrace(T));
        }
        return html;
    }

    /**
     * Finds the page for the current URL
     *
     * @return The page instance
     */
    @SuppressWarnings("WeakerAccess")
    protected IPage<?> getPageFromGuice()
    {
        IPage<?> p = get(IPage.class);
        return p;
    }

    /**
     * Does a default security check on the request
     *
     * @param req  The request
     * @param resp The response
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
    {
        //	scope.enter();
        try
        {
            perform();
        }
        catch (Exception e)
        {
            JWDefaultServlet.log.log(Level.SEVERE, "Unable to Do Get", e);
        }
        finally
        {
            //	scope.exit();
        }
    }

    /**
     * When to perform any commands
     */
    public abstract void perform();

    /**
     * The output to write to the output stream
     *
     * @param output      The specified output
     * @param contentType The content type to send out
     * @param charSet     The charset to use
     */
    public void writeOutput(StringBuilder output, String contentType, Charset charSet)
    {
        HttpServletResponse response = get(GuicedServletKeys.getHttpServletResponseKey());
        try (PrintWriter out = response.getWriter())
        {
            Date dataTransferDate = new Date();
            response.setContentType(contentType);
            response.setCharacterEncoding(charSet == null ? StandardCharsets.UTF_8
                    .toString() : charSet.displayName());
            response.setHeader(StaticStrings.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER_NAME, JWDefaultServlet.allowOrigin);
            response.setHeader(StaticStrings.ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER_NAME, "true");
            response.setHeader(StaticStrings.ACCESS_CONTROL_ALLOW_METHODS_HEADER_NAME, "GET, POST");
            response.setHeader(StaticStrings.ACCESS_CONTROL_ALLOW_HEADERS_HEADER_NAME, "Content-Type, Accept");
            out.write(output.toString());

            long transferTime = new Date().getTime() - dataTransferDate.getTime();
            JWDefaultServlet.log.log(Level.FINER, "[Network Reply Data Size]-[" + output.length() + "];");
            JWDefaultServlet.log.log(Level.FINER, "[Network Reply]-[" + output + "];[Time]-[" + transferTime + "];");
        }
        catch (IOException io)
        {
            JWDefaultServlet.log.log(Level.SEVERE, "Unable to send response to client", io);
        }
    }

    /**
     * Does a default security check on the request
     *
     * @param req  The request
     * @param resp The response
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
    {
        try
        {
            perform();
        }
        catch (Exception e)
        {
            JWDefaultServlet.log.log(Level.SEVERE, "Security Exception in Validation", e);
        }
    }

}
