/* ----------+---------------------------------------------------------
 |        /\   |     University of Heidelberg
 |       |  |  |     Department of Geography
 |      _|  |_ |     Chair of GIScience
 |    _/      \|
 |___|         |
 |             |     Berliner Str. 48
 |             |     D-69120 Heidelberg, Germany
 +-------------+----------------------------------------------------------
 <p><b>Copyright:</b> Copyright (c) 2009</p>
 <p><b>Institution:</b> University of Heidelberg, Department of Geography</p>
 @author Steffen Neubauer
 @author Michael Auer
 @version 1.1 2010-04-12
*/


 //*****************************************************************************
 //Layers by  Research Group Cartography
 //Terms of use: Users are free to use the layer for non commercial purposes.
 //Please refer to our project site www.osm-wms.de
 //*****************************************************************************

            wms_name = "WMS of Europe";
            wms_url = "http://129.206.229.147/ors-tilecache/tilecache.py?";
            wms_options = {layers: 'ors-osm',srs: 'EPSG:900913', format: 'image/png',numZoomLevels: 19};

            hs_name = "Hillshade of Europe";
            hs_url = "http://129.206.229.145/hillshade?";
            hs_options = {layers: 'europe_wms:hs_srtm_europa',srs: 'EPSG:900913', format: 'image/JPEG', transparent: 'true',numZoomLevels: 19};
            hs2_1_options = {layers: 'europe_wms:hs_srtm_europa',srs: 'EPSG:900913', format: 'image/JPEG',numZoomLevels: 19};
            
            bf_name = "Barrierfree facilities <i style='color:orange;'>*new*</i><br>(Only visible in zoomlevels<br> higher than 1:20.000)";
            bf_url = "http://129.206.229.145/geoserver/wms?";
            bf_options = {layers: 'osm_auto:points',styles: 'osm_barrierfree_01',srs: 'EPSG:900913', format: 'image/GIF', transparent: 'true',numZoomLevels: 19};

            layerMarker_options=  {styleMap: new OpenLayers.StyleMap({
                                      // Set the external graphic and background graphic images.
                                      externalGraphic: "http://129.206.229.148/europe/pix/marker-gold.png",
                                      backgroundGraphic: "http://129.206.229.148/europe/pix/marker_shadow.png",
                                      
                                      // Makes sure the background graphic is placed correctly relative
                                      // to the external graphic.
                                      graphicXOffset:-13,
                                      graphicYOffset:-25,
                                      
                                      
                                      backgroundXOffset: 0,
                                      backgroundYOffset: -20,
                                      
                                      // Set the z-indexes of both graphics to make sure the background
                                      // graphics stay in the background (shadows on top of markers looks
                                      // odd; let's not do that).
                                      graphicZIndex: 11,
                                      backgroundGraphicZIndex: 10,
                                      
                                      pointRadius: 14
                                  }),   
                                  displayInLayerSwitcher: false,
                                  isBaseLayer:false,
                                  rendererOptions: {yOrdering: true},
                                  eventListeners:{'featureadded': function(){map.getControlsByClass('OpenLayers.Control.Permalink')[0].updateLink()},
                                                  'featuresremoved': function(){map.getControlsByClass('OpenLayers.Control.Permalink')[0].updateLink()}
                                  }
                                  };
                      
 //*****************************************************************************
           
           //Overwrite the original permalink-methods, to include the "marker"-paramter in the permalink-funcionality
           
           permalinkOptions = {
        		   
       /////// creating a permalink in a special permalink box 		   
        		   
        		    draw: function() {
               			OpenLayers.Control.prototype.draw.apply(this, arguments);
                
               			if (!this.element) {
               			 
               				this.div.className = this.displayClass;
               				this.element = document.createElement("input");
               				this.element.setAttribute("type","text");
               				this.element.setAttribute("id","permaText");
               				this.element.setAttribute("name","link");
               				//this.element.innerHTML = OpenLayers.i18n("permalink");
               				//this.element.href="";
               				this.element.value="";
               				//this.div.appendChild(this.element);
               				
               				//browser switch for permalink-box
               				if (navigator.appName.indexOf("Explorer") != -1){
               					this.element.setAttribute("size","52");
               					document.getElementById("perma_ie2").appendChild(this.element);
               				}
               				else{
               					this.element.setAttribute("size","62");
               					document.getElementById("perma_other").appendChild(this.element);
               				}

               			}
               			
               			this.map.events.on({
               				'moveend': this.updateLink,
               				'changelayer': this.updateLink,
               				'changebaselayer': this.updateLink,
               				scope: this
               			})
               			
               
               // Make it so there is at least a link even though the map may not have
               // moved yet.
               			this.updateLink();
               
               			return this.div;
           	},
          
           /**
            * Method: updateLink 
            */
           			updateLink: function() {
           				var href = this.base;
           				if (href.indexOf('?') != -1) {
           					href = href.substring( 0, href.indexOf('?') );
           				}

           				href += '?' + OpenLayers.Util.getParameterString(this.createParams());
           				this.element.value = href;
           				//document.getElementById("permaText").value = href;
           				
           	    	 
           			// automatical "focus / select" update when changing the map 	
           				if (navigator.appName.indexOf("Explorer") != -1){
           	        	 var el = document.getElementById('perma_ie');
           	     			if ( el.style.display != 'none' ) {
           	     				document.getElementById("permaText").focus();
           	     				document.getElementById("permaText").select();
           	     			} 
           	    	 }
           	    	 else{
           	    		 var el = document.getElementById('perma_other');
           	    		 	if ( el.style.display != 'none' ) {
           	    			 	document.getElementById("permaText").focus();
           	    			 	document.getElementById("permaText").select();
           	    		 } 	
           	    	 }
           				
           				}, 
           
           
                    createParams: function(center, zoom, layers) {
                            center = center || this.map.getCenter();
                             
                            var params = OpenLayers.Util.getParameters(this.base);
                           
                            // If there's still no center, map is not initialized yet.
                            // Break out of this function, and simply return the params from the
                            // base link.
                            if (center) {
                    
                                //zoom
                                params.zoom = zoom || this.map.getZoom();
                    
                                //lon,lat
                                var lat = center.lat;
                                var lon = center.lon;
                               
                                if (this.displayProjection) {
                                    var mapPosition = OpenLayers.Projection.transform(
                                      { x: lon, y: lat },
                                      this.map.getProjectionObject(),
                                      this.displayProjection );
                                    lon = mapPosition.x; 
                                    lat = mapPosition.y; 
                                }       
                                params.lat = Math.round(lat*100000)/100000;
                                params.lon = Math.round(lon*100000)/100000;
                       
                                //layers       
                                layers = layers || this.map.layers; 
                                params.layers = '';
                                for (var i=0, len=layers.length; i<len; i++) {
                                    var layer = layers[i];

                                    if (layer.isBaseLayer) {
                                        params.layers += (layer == this.map.baseLayer) ? "B" : "0";
                                    } else {
                                        params.layers += (layer.getVisibility()) ? "T" : "F";           
                                    }
                                }
                                
                                //marker (inserted by Michael Auer 27.Okt.2009)
                                if(layerMarker){
                                  if (layerMarker.features.length>0){
                                    var _marker = layerMarker.features[0].clone().geometry.transform(new OpenLayers.Projection("EPSG:900913"), new OpenLayers.Projection("EPSG:4326"));
                                    params.marker = _marker.x + "," + _marker.y;
                                  }
                                  else{delete params.marker;}
                                }
                            }
                    
                            return params;
                        }
          }

 //*****************************************************************************
 //   FUNCTIONS
 //*****************************************************************************
 

  var map, measureControls,panel;
  OpenLayers.Util.onImageLoadErrorColor = "transparent";
  OpenLayers.ProxyHost ="/cgi-bin/proxy.cgi?url=";

 function init(){
      
          map = new OpenLayers.Map('map', {
              projection: new OpenLayers.Projection("EPSG:900913"),
              displayProjection: new OpenLayers.Projection("EPSG:4326"),
              units: "m",
              numZoomLevels: 19,        //16
              maxResolution: 156543.0339,
              maxExtent: new OpenLayers.Bounds(-20037508.3400,-20037508.3400,20037508.3400,20037508.3400),
              controls: [
                        new OpenLayers.Control.PanZoomBar(),
                        new OpenLayers.Control.Scale($('scale')),
                        new OpenLayers.Control.MousePosition({element: $('location')}),
                        new OpenLayers.Control.Navigation(),
                        new OpenLayers.Control.KeyboardDefaults() ,
                        new OpenLayers.Control.Permalink(null, 'http://koenigstuhl.geog.uni-heidelberg.de/osm-wms/europe.html', permalinkOptions),
						//new OpenLayers.Control.Permalink(null, 'http://131.220.111.122/europe/europe.html', permalinkOptions),
                        //new OpenLayers.Control.LayerSwitcher({'ascending':true}),
                        //new OpenLayers.Control.NavigationHistory(),
                        new OpenLayers.Control.ScaleLine($('scalebar'))
              ]
          });

          layerswitcher = new OpenLayers.Control.LayerSwitcher();
          map.addControl(layerswitcher);
          layerswitcher.maximizeControl();
          layerswitcher.div.style.top = "50px";

          var layerOSM = new OpenLayers.Layer.WMS( wms_name , wms_url , wms_options,{'buffer':2});

          var hs2 =  new OpenLayers.Layer.WMS( hs_name , hs_url , hs_options, {'buffer':2});
          hs2.setOpacity(0.23);
         // hs2.visibility=false;
          
          var hs2_1 =  new OpenLayers.Layer.WMS( hs_name , hs_url , hs2_1_options,{'buffer':2});

          var layerMapnik = new OpenLayers.Layer.OSM.Mapnik("OSM Mapnik",{'buffer':0});
          map.setBaseLayer(layerMapnik);

          var layerOsmarender = new OpenLayers.Layer.OSM.Osmarender("OSM Osmarender",{'buffer':0});

          var layerCycleMap = new OpenLayers.Layer.OSM.CycleMap("CycleMap");
          map.setBaseLayer(layerCycleMap);
          
          //Layer with barrierfree facilities (OSM-Tags: wheelchair=yes; blind=yes; capacitiy_disabled:yes)
          var layerBarrierfree = new OpenLayers.Layer.WMS( bf_name , bf_url , bf_options, {'buffer':2,isBaseLayer:false, minScale:20000});
          
          //Layer to draw a personalized marker within
          layerMarker = new OpenLayers.Layer.Vector("Marker", layerMarker_options);
           // Add a drag feature control to move features around.
           var dragFeature = new OpenLayers.Control.DragFeature(layerMarker, {onComplete:function(){map.getControlsByClass('OpenLayers.Control.Permalink')[0].updateLink()}});
           map.addControl(dragFeature);
           dragFeature.activate();

           map.addLayers([layerOSM,layerMapnik,layerOsmarender,layerCycleMap,hs2_1,hs2,layerBarrierfree,layerMarker]);

          //addMarker from URL (e.g. Permalink)
          //get Lon and Lat-Values from URLparameter "....&marker=9.56192, 50.38260"
           if (OpenLayers.Util.getParameters().marker){
              //get Lat and Lon 
              var markerPosition = String(OpenLayers.Util.getParameters().marker);
              var commaIndex = markerPosition.indexOf(",");
              var markerLon = unescape(markerPosition.substring(0,commaIndex));
              var markerLat = unescape(markerPosition.substring(commaIndex+1));
              
              //draw Marker from URLparameter
              var marker = [];
              marker.push(new OpenLayers.Feature.Vector(new OpenLayers.Geometry.Point(markerLon, markerLat).transform(new OpenLayers.Projection("EPSG:4326"), new
OpenLayers.Projection("EPSG:900913"))));
              layerMarker.addFeatures(marker);
           }
            
         


          

          //add map-click-handler
          map.events.register('click', map, handleMapClick);
          
          

          var sketchSymbolizers = {
              "Point": {
                       pointRadius: 4,
                       graphicName: "square",
                       fillColor: "white",
                       fillOpacity: 1,
                       strokeWidth: 1,
                       strokeOpacity: 1,
                       strokeColor: "#333333"
              },
              "Line": {
                       strokeWidth: 3,
                       strokeOpacity: 1,
                       strokeColor: "#666666",
                       strokeDashstyle: "dash"
                      },
              "Polygon": {
                       strokeWidth: 2,
                       strokeOpacity: 1,
                       strokeColor: "#666666",
                       fillColor: "white",
                       fillOpacity: 0.3
              }
          };

          var style = new OpenLayers.Style();
          style.addRules([
                       new OpenLayers.Rule({symbolizer: sketchSymbolizers})
          ]);
 
          var styleMap = new OpenLayers.StyleMap({"default": style});
          var options = {
              handlerOptions: {
                       style: "default", // this forces default render intent
                       layerOptions: {styleMap: styleMap},
                       persist: true
              }
          };
 
          measureControls = {
                       line: new OpenLayers.Control.Measure(
                       OpenLayers.Handler.Path, options
                       ),
                       polygon: new OpenLayers.Control.Measure(
                       OpenLayers.Handler.Polygon, options
                       )
          };
 
          var control;
 
          for(var key in measureControls) {
                  control = measureControls[key];
                  control.geodesic = true;
                  control.events.on({
                                     "measure": handleMeasurements,
                                     "measurepartial": handleMeasurements
                  });
                  map.addControl(control);
          }


          if (!map.getCenter()) map.setCenter(new OpenLayers.LonLat(1355500, 6300000),5); //map.zoomToMaxExtent();

     }//INIT ENDE
     
           // sets the HTML provided into the nodelist element
     function setHTML(response){
                nodelist_ = document.getElementById('nodelist');
                nodelist_.innerHTML = '<span style="position:relative; top:-5px;"><font size="2"><a name="featureInfo"><b>GetFeatureInfo</b></a></font> </span>' + response.responseText ;
                
                //Erste Spalte entfernen
                rows = nodelist_.getElementsByTagName('tr');
                for (var i = 0; i<rows.length; i++){
                 for(var j=0; j<rows[i].childNodes.length; j++){
                  if (rows[i].childNodes[j].nodeType==1){
                   rows[i].childNodes[j].parentNode.removeChild(rows[i].childNodes[j]);
                   break;
                  }
                 }
                }
     };
           
     function handleMapClick(evt) {

              //set Marker on click if toolbutton "setMarker" was pressed before
              if(setMarkerToggle==true){
                layerMarker.removeFeatures(layerMarker.features);
                var pixel = new OpenLayers.Pixel( evt.xy.x, evt.xy.y);
                var lonLat = map.getLonLatFromViewPortPx(pixel);
                var marker = [];
                marker.push(new OpenLayers.Feature.Vector(new OpenLayers.Geometry.Point(lonLat.lon, lonLat.lat)));
                layerMarker.addFeatures(marker);
              }

              GetFeatureInfoURI =  "http://129.206.229.145/geoserver/wms?REQUEST=GetFeatureInfo"
                                             + "&QUERY_LAYERS=osm_auto:naturals,osm_auto:waterways,osm_auto:roads,osm_auto:points,osm_auto:buildings"
                                             + "&BBOX=" + map.getExtent().toBBOX()
                                             + "&X=" +    evt.xy.x
                                             + "&Y=" + evt.xy.y
                                             + "&INFO_FORMAT=text/html"
                                             + "&FEATURE_COUNT=3"
                                             + "&FORMAT=image/png"
                                             + "&WIDTH=" + map.size.w
                                             + "&HEIGHT=" + map.size.h 
                                             + "&LAYERS=osm_auto:all"
                                             + "&SRS=EPSG:900913";
            OpenLayers.loadURL(GetFeatureInfoURI, null, 'http://131.220.111.122/cgi-bin/proxy.cgi?', setHTML, setHTML);
            OpenLayers.Event.stop(evt);

     } ;

     function calcVincenty(geometry) {
              var dist = 0;
              for (var i = 1; i < geometry.components.length; i++) {
                  var first = geometry.components[i-1];
                  var second = geometry.components[i];
                  dist += OpenLayers.Util.distVincenty(
                       {lon: first.x, lat: first.y},
                       {lon: second.x, lat: second.y}
                  );
              }
              return dist;
     }

     function handleMeasurements(event) {
              var geometry = event.geometry;
              var units = event.units;
              var order = event.order;
              var measure = event.measure;
              var element = document.getElementById('output');
              var out = "";
              if(order == 1) {
                       out += "measure: " + (measure).toFixed(1) + " " + units;
              if (map.getProjection() == "EPSG:4326") {
                          out += "<br /> Great Circle Distance: " +
                          calcVincenty(geometry).toFixed(3) + " km *";
                       }
              } else {
                       out += "measure: " + (measure).toFixed(1) + " " + units + "<sup>2</" + "sup>";
              }
              element.innerHTML = out;
     }
     
     function showPermaBox() {
    	 // browser switch for different toggling in IE and Firefox etc.
    	 if (navigator.appName.indexOf("Explorer") != -1){
        	 var el = document.getElementById('perma_ie');
     			if ( el.style.display != 'none' ) {
     				el.style.display = 'none';
     			} else {
     				el.style.display = 'inline';
     			}
    	 }
    	 else{
    		 var el = document.getElementById('perma_other');
    		 if ( el.style.display != 'none' ) {
    			 el.style.display = 'none';
    		 } else {
    			 el.style.display = 'inline';
    		 }
    	 }	
    	
    	 // marking the permalink at the <input>-tag
   		document.getElementById("permaText").focus();
    	document.getElementById("permaText").select();
     }

     function toggleControl(element) {
              for(key in measureControls) {
                      var control = measureControls[key];
                      if(element.value == key ) { //&& element.checked
                              control.activate();
                      }  else {
                              control.deactivate();
                      }
              }
              if(element.value == "setMarker" ) {setMarkerToggle=true; }
              else {setMarkerToggle=false;}
              if(element.value == "clearMarker" ) {layerMarker.removeFeatures(layerMarker.features);}
     }
     
     
    function getSize() {

      var myWidth = 0, myHeight = 0;

      if( typeof( window.innerWidth ) == 'number' ) {
          //Non-IE
          myWidth = window.innerWidth;
          myHeight = window.innerHeight;
      } else if( document.documentElement && ( document.documentElement.clientWidth || document.documentElement.clientHeight ) ) {
          //IE 6+ in 'standards compliant mode'
          myWidth = document.documentElement.clientWidth;
          myHeight = document.documentElement.clientHeight;
      } else if( document.body && ( document.body.clientWidth || document.body.clientHeight ) ) {
          //IE 4 compatible
          myWidth = document.body.clientWidth;
          myHeight = document.body.clientHeight;
      }
      return [ myWidth, myHeight ];
  }



  function getHeight() {
           var de = document.documentElement;
           var myHeight = window.innerHeight || self.innerHeight || (de&&de.clientHeight) || document.body.clientHeight;
           return myHeight;
  }