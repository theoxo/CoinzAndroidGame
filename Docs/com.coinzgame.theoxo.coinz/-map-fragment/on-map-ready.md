[com.coinzgame.theoxo.coinz](../index.md) / [MapFragment](index.md) / [onMapReady](.)

# onMapReady

`fun onMapReady(mapboxMap: <ERROR CLASS>?): Unit`

Listener function for the async call to receive the [MapboxMap](#).
Sets up the local [MapboxMap](#) instance ([mapboxMap](on-map-ready.md#com.coinzgame.theoxo.coinz.MapFragment$onMapReady()/mapboxMap)), and then begins to fetch today's coins
if they are not already cached.
Also invokes [initializeLocationLayer](#) and [initializeLocationEngine](#) which initialize
the location tracking.

### Parameters

`mapboxMap` - the received mapbox map.