package mg.miniframework.modules;

import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * Gestionnaire de métriques pour l'instrumentation du framework.
 * Collecte les compteurs de requêtes, erreurs et durées moyennes.
 */
public class MetricsManager {

    private final LongAdder totalRequests = new LongAdder();
    private final LongAdder errorRequests = new LongAdder();
    private final DoubleAdder totalDurationMs = new DoubleAdder();

    /**
     * Incrémente le compteur de requêtes totales.
     */
    public void incrementRequestCount() {
        totalRequests.increment();
    }

    /**
     * Incrémente le compteur d'erreurs.
     */
    public void incrementErrorCount() {
        errorRequests.increment();
    }

    /**
     * Ajoute la durée d'une requête (en ms) pour calculer la moyenne.
     */
    public void addRequestDuration(long durationMs) {
        totalDurationMs.add(durationMs);
    }

    /**
     * Retourne le nombre total de requêtes.
     */
    public long getTotalRequests() {
        return totalRequests.sum();
    }

    /**
     * Retourne le nombre d'erreurs.
     */
    public long getErrorRequests() {
        return errorRequests.sum();
    }

    /**
     * Calcule la durée moyenne des requêtes (en ms).
     */
    public double getAverageDurationMs() {
        long total = getTotalRequests();
        if (total == 0) return 0.0;
        return totalDurationMs.sum() / total;
    }

    /**
     * Exporte les métriques au format texte simple (pour Prometheus ou debug).
     */
    public String exportMetrics() {
        StringBuilder sb = new StringBuilder();
        sb.append("# HELP framework_requests_total Total number of requests\n");
        sb.append("# TYPE framework_requests_total counter\n");
        sb.append("framework_requests_total ").append(getTotalRequests()).append("\n");

        sb.append("# HELP framework_errors_total Total number of error responses\n");
        sb.append("# TYPE framework_errors_total counter\n");
        sb.append("framework_errors_total ").append(getErrorRequests()).append("\n");

        sb.append("# HELP framework_request_duration_avg Average request duration in milliseconds\n");
        sb.append("# TYPE framework_request_duration_avg gauge\n");
        sb.append("framework_request_duration_avg ").append(getAverageDurationMs()).append("\n");

        return sb.toString();
    }
}