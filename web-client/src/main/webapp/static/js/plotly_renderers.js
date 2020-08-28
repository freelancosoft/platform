// Generated by CoffeeScript 1.12.7
(function() {
  var callWithJQuery;

  callWithJQuery = function(pivotModule) {
    if (typeof exports === "object" && typeof module === "object") {
      return pivotModule(require("jquery"), require("plotly.js"));
    } else if (typeof define === "function" && define.amd) {
      return define(["jquery", "plotly.js"], pivotModule);
    } else {
      return pivotModule(jQuery, Plotly);
    }
  };

  callWithJQuery(function($, Plotly) {
    var CSSProps, chartMargin, chartMarginWithText, computedStyle, getAxisGridColor, getAxisLineColor, getAxisZeroLineColor, getCSSPropertyValue, getFontColor, getJoinedAttrsNames, getPaperBGColor, getPlotBGColor, makePlotlyChart, makePlotlyScatterChart;
    computedStyle = null;
    CSSProps = {
      paper_bgcolor: null,
      plot_bgcolor: null,
      font_color: null,
      axis_grid_color: null,
      axis_line_color: null,
      axis_zeroline_color: null
    };
    chartMargin = 26;
    chartMarginWithText = 50;
    getCSSPropertyValue = function(propertyName) {
      if (computedStyle === null) {
        computedStyle = getComputedStyle(document.documentElement);
      }
      return computedStyle.getPropertyValue(propertyName);
    };
    getPaperBGColor = function() {
      if (CSSProps.paper_bgcolor === null) {
        CSSProps.paper_bgcolor = getCSSPropertyValue('--background-color');
      }
      return CSSProps.paper_bgcolor;
    };
    getPlotBGColor = function() {
      if (CSSProps.plot_bgcolor === null) {
        CSSProps.plot_bgcolor = getCSSPropertyValue('--component-background-color');
      }
      return CSSProps.plot_bgcolor;
    };
    getFontColor = function() {
      if (CSSProps.font_color === null) {
        CSSProps.font_color = getCSSPropertyValue('--text-color');
      }
      return CSSProps.font_color;
    };
    getAxisGridColor = function() {
      if (CSSProps.axis_grid_color === null) {
        CSSProps.axis_grid_color = getCSSPropertyValue('--grid-separator-border-color');
      }
      return CSSProps.axis_grid_color;
    };
    getAxisLineColor = function() {
      if (CSSProps.axis_line_color === null) {
        CSSProps.axis_line_color = getCSSPropertyValue('--component-border-color');
      }
      return CSSProps.axis_line_color;
    };
    getAxisZeroLineColor = function() {
      if (CSSProps.axis_zeroline_color === null) {
        CSSProps.axis_zeroline_color = getCSSPropertyValue('--component-border-color');
      }
      return CSSProps.axis_zeroline_color;
    };
    getJoinedAttrsNames = function(attrs, opts) {
      var attr, attrsString, j, len;
      attrsString = '';
      for (j = 0, len = attrs.length; j < len; j++) {
        attr = attrs[j];
        if (attr !== opts.localeStrings.columnAttr) {
          if (attrsString !== '') {
            attrsString += ' - ';
          }
          attrsString += attr;
        }
      }
      return attrsString;
    };
    makePlotlyChart = function(reverse, traceOptions, layoutOptions, transpose) {
      if (traceOptions == null) {
        traceOptions = {};
      }
      if (layoutOptions == null) {
        layoutOptions = {};
      }
      if (transpose == null) {
        transpose = false;
      }
      return function(pivotData, opts) {
        var colKeys, columns, d, data, datumKeys, defaults, fullAggName, hAxisTitle, i, layout, legendTitle, result, rowKeys, rows, tKeys, traceKeys;
        defaults = {
          localeStrings: {
            vs: "vs",
            by: "by"
          },
          plotly: {},
          plotlyConfig: {
            responsive: true,
            locale: opts.locale
          }
        };
        opts = $.extend(true, {}, defaults, opts);
        rowKeys = pivotData.getRowKeys();
        colKeys = pivotData.getColKeys();
        if (reverse) {
          tKeys = rowKeys;
          rowKeys = colKeys;
          colKeys = tKeys;
        }
        traceKeys = transpose ? colKeys : rowKeys;
        if (traceKeys.length === 0) {
          traceKeys.push([]);
        }
        datumKeys = transpose ? rowKeys : colKeys;
        if (datumKeys.length === 0) {
          datumKeys.push([]);
        }
        fullAggName = pivotData.aggregatorName;
        if (pivotData.valAttrs.length) {
          fullAggName += "(" + (pivotData.valAttrs.join(", ")) + ")";
        }
        data = traceKeys.map(function(traceKey) {
          var datumKey, j, labels, len, trace, val, values;
          values = [];
          labels = [];
          for (j = 0, len = datumKeys.length; j < len; j++) {
            datumKey = datumKeys[j];
            val = parseFloat(pivotData.getAggregator(transpose ^ reverse ? datumKey : traceKey, transpose ^ reverse ? traceKey : datumKey).value());
            values.push(isFinite(val) ? val : null);
            labels.push(datumKey.join(' - ') || ' ');
          }
          trace = {
            name: traceKey.join(' - ') || fullAggName
          };
          if (traceOptions.type === "pie") {
            trace.values = values;
            trace.labels = labels.length > 1 ? labels : [fullAggName];
          } else {
            trace.x = transpose ? values : labels;
            trace.y = transpose ? labels : values;
          }
          return $.extend(trace, traceOptions);
        });
        if (transpose ^ reverse ^ traceOptions.type === "pie") {
          hAxisTitle = getJoinedAttrsNames(pivotData.rowAttrs, opts);
          legendTitle = getJoinedAttrsNames(pivotData.colAttrs, opts);
        } else {
          hAxisTitle = getJoinedAttrsNames(pivotData.colAttrs, opts);
          legendTitle = getJoinedAttrsNames(pivotData.rowAttrs, opts);
        }
        layout = {
          hovermode: 'closest',
          autosize: true,
          paper_bgcolor: getPaperBGColor(),
          plot_bgcolor: getPlotBGColor(),
          font: {
            color: getFontColor()
          },
          legend: {
            title: {
              text: legendTitle,
              font: {
                size: 12
              }
            }
          }
        };
        if (traceOptions.type === 'pie') {
          layout.title = {
            text: hAxisTitle,
            font: {
              size: 12
            }
          };
          layout.margin = {
            l: chartMargin,
            r: chartMargin,
            t: hAxisTitle !== "" ? chartMarginWithText : chartMargin,
            b: chartMargin
          };
          columns = Math.ceil(Math.sqrt(data.length));
          rows = Math.ceil(data.length / columns);
          layout.grid = {
            columns: columns,
            rows: rows
          };
          for (i in data) {
            d = data[i];
            d.domain = {
              row: Math.floor(i / columns),
              column: i - columns * Math.floor(i / columns)
            };
            if (data.length > 1) {
              d.title = d.name;
            }
          }
          if (data[0].labels.length === 1) {
            layout.showlegend = false;
          }
        } else {
          layout.xaxis = {
            title: {
              text: transpose ? fullAggName : hAxisTitle,
              font: {
                size: 12
              }
            },
            automargin: true,
            gridcolor: getAxisGridColor(),
            linecolor: getAxisLineColor(),
            zerolinecolor: getAxisZeroLineColor()
          };
          if (transpose || hAxisTitle !== "") {
            layout.xaxis.title.standoff = 10;
          }
          layout.yaxis = {
            title: {
              text: transpose ? hAxisTitle : fullAggName,
              font: {
                size: 12
              }
            },
            automargin: true,
            gridcolor: getAxisGridColor(),
            linecolor: getAxisLineColor(),
            zerolinecolor: getAxisZeroLineColor()
          };
          layout.margin = {
            l: !transpose || hAxisTitle !== '' ? chartMarginWithText : chartMargin,
            r: chartMargin,
            t: chartMargin,
            b: transpose || hAxisTitle !== "" ? chartMarginWithText : chartMargin
          };
        }
        result = $("<div>").appendTo($("body"));
        Plotly.newPlot(result[0], data, $.extend(layout, layoutOptions, opts.plotly), opts.plotlyConfig);
        return result.detach();
      };
    };
    makePlotlyScatterChart = function() {
      return function(pivotData, opts) {
        var colAttrsString, colKey, colKeys, data, defaults, j, k, layout, len, len1, renderArea, result, rowAttrsString, rowKey, rowKeys, v;
        defaults = {
          localeStrings: {
            vs: "vs",
            by: "by"
          },
          plotly: {},
          plotlyConfig: {
            responsive: true,
            locale: opts.locale
          }
        };
        opts = $.extend(true, {}, defaults, opts);
        rowKeys = pivotData.getRowKeys();
        if (rowKeys.length === 0) {
          rowKeys.push([]);
        }
        colKeys = pivotData.getColKeys();
        if (colKeys.length === 0) {
          colKeys.push([]);
        }
        data = {
          x: [],
          y: [],
          text: [],
          type: 'scatter',
          mode: 'markers'
        };
        for (j = 0, len = rowKeys.length; j < len; j++) {
          rowKey = rowKeys[j];
          for (k = 0, len1 = colKeys.length; k < len1; k++) {
            colKey = colKeys[k];
            v = pivotData.getAggregator(rowKey, colKey).value();
            if (v != null) {
              data.x.push(colKey.join(' - '));
              data.y.push(rowKey.join(' - '));
              data.text.push(v);
            }
          }
        }
        colAttrsString = getJoinedAttrsNames(pivotData.colAttrs, opts);
        rowAttrsString = getJoinedAttrsNames(pivotData.rowAttrs, opts);
        layout = {
          margin: {
            l: rowAttrsString === '' ? chartMargin : void 0,
            r: chartMargin,
            t: chartMargin,
            b: colAttrsString === '' ? chartMargin : void 0
          },
          hovermode: 'closest',
          xaxis: {
            title: {
              text: colAttrsString,
              font: {
                size: 12
              }
            },
            automargin: true
          },
          yaxis: {
            title: {
              text: rowAttrsString,
              font: {
                size: 12
              },
              automargin: true
            }
          },
          autosize: true,
          paper_bgcolor: getPaperBGColor(),
          plot_bgcolor: getPlotBGColor(),
          font: {
            color: getFontColor()
          }
        };
        if (colAttrsString !== '') {
          layout.xaxis.title.standoff = 10;
        }
        renderArea = $("<div>", {
          style: "display:none;"
        }).appendTo($("body"));
        result = $("<div>").appendTo(renderArea);
        Plotly.newPlot(result[0], [data], $.extend(layout, opts.plotly), opts.plotlyConfig);
        result.detach();
        renderArea.remove();
        return result;
      };
    };
    $.pivotUtilities.plotly_renderers = {
      "BARCHART": makePlotlyChart(true, {
        type: 'bar'
      }, {
        barmode: 'group'
      }, false),
      "STACKED_BARCHART": makePlotlyChart(true, {
        type: 'bar'
      }, {
        barmode: 'relative'
      }, false),
      "LINECHART": makePlotlyChart(true, {}, {}, false),
      "AREACHART": makePlotlyChart(true, {
        stackgroup: 1
      }, {}, false),
      "SCATTERCHART": makePlotlyScatterChart(),
      "MULTIPLE_PIECHART": makePlotlyChart(false, {
        type: 'pie',
        scalegroup: 1,
        hoverinfo: 'label+value',
        textinfo: 'none'
      }, {}, true),
      "HORIZONTAL_BARCHART": makePlotlyChart(true, {
        type: 'bar',
        orientation: 'h'
      }, {
        barmode: 'group'
      }, true),
      "HORIZONTAL_STACKED_BARCHART": makePlotlyChart(true, {
        type: 'bar',
        orientation: 'h'
      }, {
        barmode: 'relative'
      }, true)
    };
    return $.pivotUtilities.colorThemeChanged = function(plot) {
      var relayout;
      computedStyle = null;
      CSSProps.paper_bgcolor = null;
      CSSProps.plot_bgcolor = null;
      CSSProps.font_color = null;
      CSSProps.axis_grid_color = null;
      CSSProps.axis_line_color = null;
      CSSProps.axis_zeroline_color = null;
      if (plot !== void 0) {
        relayout = function() {
          var update;
          update = {
            paper_bgcolor: getPaperBGColor(),
            plot_bgcolor: getPlotBGColor(),
            font: {
              color: getFontColor()
            },
            xaxis: {
              gridcolor: getAxisGridColor(),
              linecolor: getAxisLineColor(),
              zerolinecolor: getAxisZeroLineColor()
            },
            yaxis: {
              gridcolor: getAxisGridColor(),
              linecolor: getAxisLineColor(),
              zerolinecolor: getAxisZeroLineColor()
            }
          };
          return Plotly.relayout(plot, update);
        };
        return setTimeout(relayout);
      }
    };
  });

}).call(this);

//# sourceMappingURL=plotly_renderers.js.map
