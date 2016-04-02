var adaptiveNodes = [];
var adaptiveLinks = [];

function adaptiveInit() {
    //if (window.goSamples) goSamples();  // init for these samples -- you don't need to call this
    var $ = go.GraphObject.make;  // for conciseness in defining templates

    adaptiveDiagram =
        $(go.Diagram, "adaptiveDiagram",  // must name or refer to the DIV HTML element
            {
                // start everything in the middle of the viewport
                contentAlignment: go.Spot.Center,
                autoScale: go.Diagram.Uniform,
                isEnabled: false
                // have mouse wheel events zoom in and out instead of scroll up and down
                //"toolManager.mouseWheelBehavior": go.ToolManager.WheelZoom,
                // support double-click in background creating a new node
                //"clickCreatingTool.archetypeNodeData": { text: "new node" },
                // enable undo & redo
                //"undoManager.isEnabled": true
                //"animationManager.isEnabled": false
            });

    // when the document is modified, add a "*" to the title and enable the "Save" button
    //adaptiveDiagram.addDiagramListener("Modified", function(e) {
    //    var button = document.getElementById("SaveButton");
    //    if (button) button.disabled = !adaptiveDiagram.isModified;
    //    var idx = document.title.indexOf("*");
    //    if (adaptiveDiagram.isModified) {
    //        if (idx < 0) document.title += "*";
    //    } else {
    //        if (idx >= 0) document.title = document.title.substr(0, idx);
    //    }
    //});

    // define the Node template
    adaptiveDiagram.nodeTemplate =
        $(go.Node, "Auto",
            new go.Binding("location", "loc", go.Point.parse).makeTwoWay(go.Point.stringify),
            // define the node's outer shape, which will surround the TextBlock
            $(go.Shape, "Rectangle",
                {
                    fill: "white",
                    stroke: "white",
                    cursor: "pointer"
                },
                new go.Binding("fill", "color")),
            $(go.Panel, "Vertical",
                {margin: 6},
                $(go.TextBlock,
                    {
                        font: "bold 11pt helvetica, bold arial, sans-serif",
                        margin: 8
                    },
                    new go.Binding("text", "text").makeTwoWay(),
                    new go.Binding("stroke", "textColor"))
            )
        );

    //// unlike the normal selection Adornment, this one includes a Button
    //adaptiveDiagram.nodeTemplate.selectionAdornmentTemplate =
    //    $(go.Adornment, "Spot",
    //        $(go.Panel, "Auto",
    //            $(go.Shape, {fill: null, stroke: "blue", strokeWidth: 2}),
    //            $(go.Placeholder)  // this represents the selected Node
    //        )
    //    ); // end Adornment


    // replace the default Link template in the linkTemplateMap
    adaptiveDiagram.linkTemplate =
        $(go.Link,  // the whole link panel
            //{ curve: go.Link.Bezier, adjusting: go.Link.Stretch, reshapable: true },
            //new go.Binding("curviness", "curviness"),
            //new go.Binding("points").makeTwoWay(),
            $(go.Shape,  // the link shape
                {strokeWidth: 1.5}),
            $(go.Shape,  // the arrowhead
                {toArrow: "standard", stroke: null}),
            $(go.Panel, "Auto",// { segmentOffset: new go.Point(0, -10) },,
                new go.Binding("segmentOffset", "trl", function (s) {
                    var parts = s.split(' ');
                    return new go.Point(parseInt(parts[0]), parseInt(parts[1]));
                }),
                $(go.Shape,  // the link shape
                    {
                        fill: "transparent",
                        stroke: null
                    }),
                $(go.TextBlock, "transition",  // the label
                    {
                        textAlign: "center",
                        font: "10pt helvetica, arial, sans-serif",
                        //stroke: "black",
                        margin: 4
                    },
                    new go.Binding("text", "text").makeTwoWay(),
                    new go.Binding("stroke", "color"))
            )
        );

    adaptiveDiagram.model = new go.GraphLinksModel(
        adaptiveNodes,
        adaptiveLinks);
}

function updateAdaptiveModel(nodeList, linkList, cars) {
    $.each(nodeList, function () {
        var data = adaptiveDiagram.model.findNodeDataForKey(this.key);
        // This will update the color of the "Delta" Node
        if (data !== null) {
            adaptiveDiagram.model.setDataProperty(data, "color", this.color);
            adaptiveDiagram.model.setDataProperty(data, "textColor", this.textColor);
            adaptiveDiagram.model.setDataProperty(data, "tests", this.tests);
            adaptiveDiagram.model.setDataProperty(data, "adaptations", this.adaptations);
        }
        else
            adaptiveDiagram.model.addNodeData(this)
    });
    $.each(linkList, function (index, link) {
        //var data = adaptiveDiagram.model.findLinkForData(this);
        //// This will update the color of the "Delta" Node
        //if (data !== null) {
        $.each($.grep(adaptiveDiagram.model.linkDataArray, function (e) {
            return e.from == link.from && e.to == link.to;
        }), function (idx, item) {
            adaptiveDiagram.model.removeLinkData(item)
        });
        //adaptiveDiagram.model.removeLinkData(this);
        adaptiveDiagram.model.addLinkData(link);
        //}
    });

    if (lastAdaptiveCarsCount < cars.count) {
        lastAdaptiveCarsCount = cars.count;
        chart.series[1].addPoint([cars.count, cars.avgArivalTime], true, false);
    }
}