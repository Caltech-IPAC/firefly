/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.net;

import edu.caltech.ipac.util.download.NetParams;

public class ResolverParams implements NetParams {

    private final String name;
    private final Resolver resolver;

    public ResolverParams(String name, Resolver resolver) {
        this.name= name;
        this.resolver= resolver;
    }

    public String getName() { return name; }
    public Resolver getResolver() { return resolver; }

    public String getUniqueString() {
        return resolver.toString()+"__ATTRIBUTE_" + name;
    }

    public String toString() {
       return getUniqueString();
    }
}
