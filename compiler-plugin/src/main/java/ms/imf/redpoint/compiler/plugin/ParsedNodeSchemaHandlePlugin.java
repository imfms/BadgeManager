package ms.imf.redpoint.compiler.plugin;

public interface ParsedNodeSchemaHandlePlugin {

    /**
     * @param pluginContext plugin context
     * @throws AptProcessException framework will show your error to user friendly
     */
    void onParsed(PluginContext pluginContext) throws AptProcessException;
    
}
