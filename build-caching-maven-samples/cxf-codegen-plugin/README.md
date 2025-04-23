Example configuration for making the `wsdl2java` goal of the `cxf-codegen-plugin` cacheable.

> [!WARNING]
> The `wsdlOptions` and `defaultOptions` can be used for advanced configuration of the `cxf-codegen-plugin`, see [documentation](https://cxf.apache.org/docs/wsdl-to-java.html). However, only the `extraargs` and `bindingfiles` inner attributes are supported in the caching configuration here. 
> It means that if you use other attributes inside the `wsdlOptions` and `defaultOptions` sections, you may get cache hits when those attributes (or the file referenced in those attributes) are modified.  
