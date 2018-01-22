// geohash.js
// Geohash library for Javascript
// (c) 2008 David Troy
// Distributed under the MIT License

BITS = [16, 8, 4, 2, 1];

BASE32 = 											   "0123456789bcdefghjkmnpqrstuvwxyz";
NEIGHBORS = { right  : { even :  "bc01fg45238967deuvhjyznpkmstqrwx" },
							left   : { even :  "238967debc01fg45kmstqrwxuvhjyznp" },
							top    : { even :  "p0r21436x8zb9dcf5h7kjnmqesgutwvy" },
							bottom : { even :  "14365h7k9dcfesgujnmqp0r2twvyx8zb" } };
BORDERS   = { right  : { even : "bcfguvyz" },
							left   : { even : "0145hjnp" },
							top    : { even : "prxz" },
							bottom : { even : "028b" } };

NEIGHBORS.bottom.odd = NEIGHBORS.left.even;
NEIGHBORS.top.odd = NEIGHBORS.right.even;
NEIGHBORS.left.odd = NEIGHBORS.bottom.even;
NEIGHBORS.right.odd = NEIGHBORS.top.even;

BORDERS.bottom.odd = BORDERS.left.even;
BORDERS.top.odd = BORDERS.right.even;
BORDERS.left.odd = BORDERS.bottom.even;
BORDERS.right.odd = BORDERS.top.even;

function refine_interval(interval, cd, mask) {
	if (cd&mask)
		interval[0] = (interval[0] + interval[1])/2;
  else
		interval[1] = (interval[0] + interval[1])/2;
}

function calculateAdjacent(srcHash, dir) {
	srcHash = srcHash.toLowerCase();
	var lastChr = srcHash.charAt(srcHash.length-1);
	var type = (srcHash.length % 2) ? 'odd' : 'even';
	var base = srcHash.substring(0,srcHash.length-1);
	if (BORDERS[dir][type].indexOf(lastChr)!==-1)
		base = calculateAdjacent(base, dir);
	return base + BASE32[NEIGHBORS[dir][type].indexOf(lastChr)];
}

function decodeGeoHash(geohash) {
	var is_even = 1;
	var lat = []; var lon = [];
	lat[0] = -90.0;  lat[1] = 90.0;
	lon[0] = -180.0; lon[1] = 180.0;
	lat_err = 90.0;  lon_err = 180.0;
	
	for (i=0; i<geohash.length; i++) {
		c = geohash[i];
		cd = BASE32.indexOf(c);
		for (j=0; j<5; j++) {
			mask = BITS[j];
			if (is_even) {
				lon_err /= 2;
				refine_interval(lon, cd, mask);
			} else {
				lat_err /= 2;
				refine_interval(lat, cd, mask);
			}
			is_even = !is_even;
		}
	}
	lat[2] = (lat[0] + lat[1])/2;
	lon[2] = (lon[0] + lon[1])/2;

	return { latitude: lat, longitude: lon};
}

function encodeGeoHash(latitude, longitude) {
	var is_even=1;
	var i=0;
	var lat = []; var lon = [];
	var bit=0;
	var ch=0;
	var precision = 12;
	geohash = "";

	lat[0] = -90.0;  lat[1] = 90.0;
	lon[0] = -180.0; lon[1] = 180.0;
	
	while (geohash.length < precision) {
	  if (is_even) {
			mid = (lon[0] + lon[1]) / 2;
	    if (longitude > mid) {
				ch |= BITS[bit];
				lon[0] = mid;
	    } else
				lon[1] = mid;
	  } else {
			mid = (lat[0] + lat[1]) / 2;
	    if (latitude > mid) {
				ch |= BITS[bit];
				lat[0] = mid;
	    } else
				lat[1] = mid;
	  }

		is_even = !is_even;
	  if (bit < 4)
			bit++;
	  else {
			geohash += BASE32[ch];
			bit = 0;
			ch = 0;
	  }
	}
	return geohash;
}

// FIM GEOHASH






// The Cloud Functions for Firebase SDK to create Cloud Functions and setup triggers.
const functions = require('firebase-functions');

// The Firebase Admin SDK to access the Firebase Realtime Database.
const admin = require('firebase-admin');
admin.initializeApp(functions.config().firebase);


// Listen for changes in all documents and all subcollections
exports.useMultipleWildcards = functions.firestore
  .document('groups/{group}/places/{place}')
  .onWrite((event) => {
 // Retrieve the current and previous value
    const data = event.data.data();
//    const previousData = event.data.previous.data();
    console.log("dados a serem persistidos");   
    console.log(data);

    // We'll only update if the name has changed.
    // This is crucial to prevent infinite loops.
    if (data.geohash) return;
    const latitude = data.latitude;
    const longitude = data.longitude;
	var geohashCalculado = encodeGeoHash(latitude, longitude);

if (data.state){
    return event.data.ref.set(
            {geohash: geohashCalculado
                }, {merge: true});
} else {
// inicio geocoding para obter cidade e estado
//https://www.npmjs.com/package/node-geocoder
    var endereco_formatado;
    var cidade;
    var estado;
    var NodeGeocoder = require('node-geocoder');
     
    var options = {
      provider: 'google',
      // Optional depending on the providers
      httpAdapter: 'https', // Default
      apiKey: 'AIzaSyD8Cs4Anm6h6AeEsSOV2vM0q-0Iw8jhhic', // for Mapquest, OpenCage, Google Premier
      formatter: null         // 'gpx', 'string', ...
    };
     
    var geocoder = NodeGeocoder(options);

    return geocoder.reverse({lat:latitude, lon:longitude})
          .then(function(res) {
            console.log(res[0]);
            json_object = res[0]; //convert to an object
            endereco_formatado = json_object.formattedAddress;
            adm_levels = json_object.administrativeLevels;
            cidade = adm_levels.level2long;
            estado = adm_levels.level1long;
            return event.data.ref.set({geohash: geohashCalculado,
                    address: endereco_formatado,
                    city: cidade,
                    state: estado,
                    'postal-code': json_object.zipcode,
                    location_debug: JSON.stringify(res[0])
                    }, {merge: true});
          })
          .catch(function(err) {
            console.log(err);
          });

}
   
  });

// // Create and Deploy Your First Cloud Functions
// // https://firebase.google.com/docs/functions/write-firebase-functions
//
// exports.helloWorld = functions.https.onRequest((request, response) => {
//  response.send("Hello from Firebase!");
// });

// geohash.js
// Geohash library for Javascript
// (c) 2008 David Troy
// Distributed under the MIT License

