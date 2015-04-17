package com.hello.suripu.service.resources;

import com.hello.suripu.core.resources.BaseResource;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BaseResourceTest {

    @Test
         public void testGetEmptyIP() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("");
        when(request.getRemoteAddr()).thenReturn("");

        final String ipAddress = BaseResource.getIpAddress(request);
        assertThat(ipAddress, is(""));
    }

    @Test
    public void testGetNullIP() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn(null);

        final String ipAddress = BaseResource.getIpAddress(request);
        assertThat(ipAddress, is(""));
    }

    @Test
    public void testGetMultipleIP() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("127.0.0.1,127.0.0.2");
        when(request.getRemoteAddr()).thenReturn(null);

        final String ipAddress = BaseResource.getIpAddress(request);
        assertThat(ipAddress, is("127.0.0.1"));
    }
}
