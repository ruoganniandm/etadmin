/*
 *  Copyright 2019-2020 Zheng Jie
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.eternity.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.*;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/**
 * WebMvcConfigurer
 *
 * @author Zheng Jie
 * @date 2018-11-30
 */
@Configuration
@EnableWebMvc
public class ConfigurerAdapter implements WebMvcConfigurer {

    /** 文件配置 */
    private final FileProperties properties;

    public ConfigurerAdapter(FileProperties properties) {
        this.properties = properties;
    }

    /**
     * 方法类	            方法名称	               必填	        请求头字段	                     说明
     * CorsRegistry	    addMapping	                是	无, 非Cors属性,属于SpringBoot配置	    配置支持跨域的路径
     * CorsRegistration	allowedOrigins	            是	Access-Control-Allow-Origin	        配置允许的源
     * CorsRegistration	addAllowedOriginPattern	    是	Access-Control-Allow-Origin	        配置允许的源。Spring Boot 2.4.0之后，allowedOrigins不允许使用*，改用这个
     * CorsRegistration	allowedMethods	            是	Access-Control-Allow-Methods	    配置支持跨域请求的方法,如：GET、POST，一次性返回
     * CorsRegistration	maxAge	                    否	Access-Control-Max-Age	            配置预检请求的有效时间
     * CorsRegistration	allowCredentials	        否	Access-Control-Allow-Credentials	配置是否允许发送Cookie, 用于 凭证请求
     * CorsRegistration	allowedHeaders	            否	Access-Control-Request-Headers	    配置允许的自定义请求头, 用于 预检请求
     * CorsRegistration	exposedHeaders	            否	Access-Control-Expose-Headers	    配置响应的头信息,在其中可以设置其他的头信息
     */
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        FileProperties.ElPath path = properties.getPath();
        String avatarUtl = "file:" + path.getAvatar().replace("\\","/");
        String pathUtl = "file:" + path.getPath().replace("\\","/");
        registry.addResourceHandler("/avatar/**").addResourceLocations(avatarUtl).setCachePeriod(0);
        registry.addResourceHandler("/file/**").addResourceLocations(pathUtl).setCachePeriod(0);
        registry.addResourceHandler("/**").addResourceLocations("classpath:/META-INF/resources/").setCachePeriod(0);
    }

    /**
     * 通用拦截器排除设置，所有拦截器都会自动加springdoc-openapi相关的资源排除信息，不用在应用程序自身拦截器定义的地方去添加，算是良心解耦实现。
     */
    @SuppressWarnings("unchecked")
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        try {
            Field registrationsField = FieldUtils.getField(InterceptorRegistry.class, "registrations", true);
            List<InterceptorRegistration> registrations = (List<InterceptorRegistration>) ReflectionUtils.getField(registrationsField, registry);
            if (registrations != null) {
                for (InterceptorRegistration interceptorRegistration : registrations) {
                    interceptorRegistration.excludePathPatterns("/v3/api-docs/**", "/swagger-ui/**");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 对向前端返回的数据，日期格式化。
     *
     * @param converters /
     * @date 2021/11/25 10:04 上午
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        List<MediaType> supportedMediaTypes = new ArrayList<>();
        supportedMediaTypes.add(MediaType.APPLICATION_JSON);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof MappingJackson2HttpMessageConverter) {
                MappingJackson2HttpMessageConverter jsonMessageConverter = (MappingJackson2HttpMessageConverter) converter;
                ObjectMapper objectMapper = jsonMessageConverter.getObjectMapper();
                objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                objectMapper.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
                objectMapper.setDateFormat(dateFormat);
                objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
                objectMapper.registerModule(new JavaTimeModule());
                jsonMessageConverter.setSupportedMediaTypes(supportedMediaTypes);
                break;
            }
        }
    }

    /**
     * 对向前端返回的数据，日期格式化。
     *
     * @param converters /
     * @date 2021/11/25 10:04 上午
     */

//    @Override
//    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
//        // 使用 fastjson 序列化，会导致 @JsonIgnore 失效，可以使用 @JSONField(serialize = false) 替换
//        FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();
//        List<MediaType> supportMediaTypeList = new ArrayList<>();
//        supportMediaTypeList.add(MediaType.APPLICATION_JSON);
//        FastJsonConfig config = new FastJsonConfig();
//
//        config.setDateFormat("yyyy-MM-dd HH:mm:ss");
//        config.setSerializerFeatures(SerializerFeature.DisableCircularReferenceDetect);
//        converter.setFastJsonConfig(config);
//        converter.setSupportedMediaTypes(supportMediaTypeList);
//        converter.setDefaultCharset(StandardCharsets.UTF_8);
//        converters.add(converter);
//    }


}
