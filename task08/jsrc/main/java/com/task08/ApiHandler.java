package com.task08;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.lambda.LambdaLayer;
import com.syndicate.deployment.annotations.lambda.LambdaUrlConfig;
import com.syndicate.deployment.model.ArtifactExtension;
import com.syndicate.deployment.model.DeploymentRuntime;
import com.syndicate.deployment.model.RetentionSetting;
import com.syndicate.deployment.model.lambda.url.AuthType;
import com.syndicate.deployment.model.lambda.url.InvokeMode;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.io.File;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@LambdaHandler(lambdaName = "api_handler",
        roleName = "api_handler-role",
        isPublishVersion = true,
        logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED,
        layers = {"sdk_layer"},
        runtime = DeploymentRuntime.JAVA11
)

@LambdaUrlConfig(authType = AuthType.NONE, invokeMode = InvokeMode.BUFFERED)
@LambdaLayer(layerName = "sdk_layer",
        runtime = DeploymentRuntime.JAVA11,
        artifactExtension = ArtifactExtension.ZIP,
        libraries = {"lib/task08-1.0.0.jar"}
)
public class ApiHandler implements RequestHandler<Object, Map<String, Object>> {

    public Map<String, Object> handleRequest(Object request, Context context) {

        File file = new File("/opt/java/lib/task08-1.0.0.jar");
        URL url = null;
        try {
            url = file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        URL[] urls = new URL[]{url};
        ClassLoader cl = new URLClassLoader(urls);
        Class cls = null;
        try {
            cls = cl.loadClass("com.task08.OpenMeteoApi");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        Object openMeteoObj = null;
        try {
            openMeteoObj = cls.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        Method method = null;
        try {
            method = cls.getDeclaredMethod("getWeatherForecast");
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        CompletableFuture<String> weather = null;
        try {
            weather = (CompletableFuture<String>) method.invoke(openMeteoObj, new Object[]{});
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put("statusCode", 200);
        resultMap.put("body", weather.join());
        return resultMap;
    }
}