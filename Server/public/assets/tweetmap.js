    var socket, map, heatmap, mapPoints, markers = [];

    function initMap() {
      map = new google.maps.Map(document.getElementById('map'), {
        zoom: 2,
        center: {lat: 0, lng: 0},
        mapTypeId: google.maps.MapTypeId.HYBRID
      });
      initHeatMap();
      establishConnection();
    }

    function initHeatMap() {
      getMapPoints();
      heatmap = new google.maps.visualization.HeatmapLayer({
        data: mapPoints,
        map: map
      });
    }

    function getMapPoints() {
      temp = getPoints();
      mapPoints = new google.maps.MVCArray(temp);
    }

/*
    function filterMapPoints() {
      while(mapPoints.length) {
         mapPoints.pop();
      }
      while(markers.length) {
         var marker = markers.pop();
         marker.setMap(null);
      }
    }
*/

    function updateMapPoints(coordinates) {
      mapPoints.push(new google.maps.LatLng(coordinates[1], coordinates[0]));
    }

    function toggleHeatmap() {
      heatmap.setMap(heatmap.getMap() ? null : map);
    }

    function changeGradient() {
      var gradient = [
        'rgba(0, 255, 255, 0)',
        'rgba(0, 255, 255, 1)',
        'rgba(0, 191, 255, 1)',
        'rgba(0, 127, 255, 1)',
        'rgba(0, 63, 255, 1)',
        'rgba(0, 0, 255, 1)',
        'rgba(0, 0, 223, 1)',
        'rgba(0, 0, 191, 1)',
        'rgba(0, 0, 159, 1)',
        'rgba(0, 0, 127, 1)',
        'rgba(63, 0, 91, 1)',
        'rgba(127, 0, 63, 1)',
        'rgba(191, 0, 31, 1)',
        'rgba(255, 0, 0, 1)'
      ]
      heatmap.set('gradient', heatmap.get('gradient') ? null : gradient);
    }

    function changeRadius() {
      heatmap.set('radius', heatmap.get('radius') ? null : 20);
    }

    function changeOpacity() {
      heatmap.set('opacity', heatmap.get('opacity') ? null : 0.2);
    }

    function getPoints() {
      var icon = "http://maps.google.com/mapfiles/ms/icons/blue-dot.png";
      var marker = new google.maps.Marker({
		     position: {lat: 37.46, lng: -122.26},
		     map: map,
		     icon: icon,
                   });
      return [
        new google.maps.LatLng(37.46, -122.26),
      ];
    }

    function establishConnection() {
      socket = io.connect();
      socket.on('mapdata', function(json_tweet) {
        markSentiment(json_tweet);
        updateMapPoints(json_tweet.coordinates.coordinates);
      });
    }

    function markSentiment(json_tweet) {
      var sentiment = json_tweet.sentimentType;
      var icon = "http://maps.google.com/mapfiles/ms/icons/yellow-dot.png";
	if (sentiment == "positive") {
          icon = "http://maps.google.com/mapfiles/ms/icons/green-dot.png";
	} else if (sentiment == "negative") {
	  icon = "http://maps.google.com/mapfiles/ms/icons/red-dot.png";
	}
        var marker = new google.maps.Marker({
		     position: {lat: json_tweet.coordinates.coordinates[1], lng: json_tweet.coordinates.coordinates[0]},
		     map: map,
		     icon: icon,
		     title: json_tweet.text
	             });
        markers.push(marker);
    };

    function getTrendsById(geoid) {
      socket.emit('gettrends', {id: geoid});
    };
