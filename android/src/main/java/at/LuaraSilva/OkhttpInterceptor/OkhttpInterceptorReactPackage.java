package at.LuaraSilva.OkhttpInterceptor;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OkhttpInterceptorPackage implements ReactPackage {
    @Override
    public List<NativeModule> createNativeModules(ReactApplicationContext reactContext) {
        List<NativeModule> modules = new ArrayList<>();
        modules.add(new OkhttpInterceptorReactModule(reactContext));
        return modules;
    }

    @Override
    public List<ViewManager<?, ?>> createViewManagers(ReactApplicationContext reactContext) {
        return Collections.emptyList();
    }
}


/*____________________________________________

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;
import com.facebook.react.bridge.JavaScriptModule;
import at.alwinschuster.HttpServer.HttpServerModule;

public class HttpServerReactPackage implements ReactPackage {
  //In newer versions of React Native (since 0.47+), the createJSModules method has been deprecated. This method is no longer needed, so you can safely remove it. React Native automatically registers JavaScript modules for you.
  @Override
  public List<NativeModule> createNativeModules(ReactApplicationContext reactContext) {
    List<NativeModule> modules = new ArrayList<>();
    modules.add(new HttpServerModule(reactContext));
    return modules;
  }

  public List<Class<? extends JavaScriptModule>> createJSModules() {
  	return Collections.emptyList();
  }

  @Override
  public List<ViewManager> createViewManagers(ReactApplicationContext reactContext) {
  	return Collections.emptyList();
  }
}
*/