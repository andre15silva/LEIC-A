package pt.tecnico.sauron.silo.client;

import pt.tecnico.sauron.silo.grpc.CameraDto;
import pt.tecnico.sauron.silo.grpc.ObservationDto;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ResponseCache {
    Map<String, List<ObservationDto>> observationCache = new HashMap<>();
    Map<String, CameraDto> cameraCache = new HashMap<>();
    int maxSize = 0;
    int currentSize = 0;
    LinkedList<String> responseOrder = new LinkedList<>(); // Queue

    /**
     * Constructs a cache with size zero
     */
    public ResponseCache() {
    }

    /**
     * Constructs a LIFO cache with size maxSize
     *
     * @param maxSize Maximum size of the cache
     */
    public ResponseCache(int maxSize) {
        this.maxSize = maxSize;
    }

    /**
     * Adds a (request,response) pair of observation type to the cache
     *
     * @param request  The request
     * @param response The response
     */
    public void addResponse(String request, List<ObservationDto> response) {
        if (maxSize == 0) return;

        if (observationCache.get(request) == null) {
            // If cache is full remove oldest response (FIFO)
            if (this.currentSize == this.maxSize) {
                String oldestResponse = responseOrder.remove();

                // Remove oldest response
                if (observationCache.get(oldestResponse) != null) {
                    observationCache.remove(oldestResponse);
                } else {
                    cameraCache.remove(oldestResponse);
                }

                this.currentSize--;
            }

            observationCache.put(request, response);
            responseOrder.add(request);
            this.currentSize++;
        } else { // response updated
            observationCache.replace(request, response);

            // get request to end of queue
            responseOrder.remove(request);
            responseOrder.add(request);
        }
    }

    /**
     * Adds a (request,response) pair of camera type to the cache
     *
     * @param request  The request
     * @param response The response
     */
    public void addResponse(String request, CameraDto response) {
        if (maxSize == 0) return;

        if (cameraCache.get(request) == null) {
            // If cache is full remove oldest response (FIFO)
            if (this.currentSize == this.maxSize) {
                String oldestResponse = responseOrder.remove();

                // Remove oldest response
                if (observationCache.get(oldestResponse) != null) {
                    observationCache.remove(oldestResponse);
                } else {
                    cameraCache.remove(oldestResponse);
                }

                this.currentSize--;
            }

            cameraCache.put(request, response);
            responseOrder.add(request);
            this.currentSize++;
        } else { // response updated
            cameraCache.replace(request, response);

            // get request to end of queue
            responseOrder.remove(request);
            responseOrder.add(request);
        }
    }

    /**
     * Queries the cache for the result of an observation request
     *
     * @param request The request
     * @return The result of the cache query
     */
    public List<ObservationDto> getObservationResponse(String request) {
        return observationCache.get(request);
    }

    /**
     * Queries the cache for the result of a camera request
     *
     * @param request The request
     * @return The result of the cache query
     */
    public CameraDto getCameraResponse(String request) {
        return cameraCache.get(request);
    }
}
