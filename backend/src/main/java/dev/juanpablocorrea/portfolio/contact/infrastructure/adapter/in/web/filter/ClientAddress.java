package dev.juanpablocorrea.portfolio.contact.infrastructure.adapter.in.web.filter;

/**
 * Finds the address of the real visitor behind reverse proxies.
 *
 * <p>Each trusted proxy appends the address it saw to {@code X-Forwarded-For}. Anything
 * to the left of those entries was written by the client and can be forged, so only the
 * entry added by the outermost trusted proxy is used.
 */
public final class ClientAddress {

    private ClientAddress() {
    }

    public static String resolve(String forwardedFor, String remoteAddress, int trustedProxyHops) {
        if (trustedProxyHops > 0 && forwardedFor != null && !forwardedFor.isBlank()) {
            String[] hops = forwardedFor.split(",");
            int index = Math.max(0, hops.length - trustedProxyHops);
            String candidate = hops[index].strip();
            if (!candidate.isEmpty()) {
                return candidate;
            }
        }
        return remoteAddress == null || remoteAddress.isBlank() ? "unknown" : remoteAddress;
    }
}
