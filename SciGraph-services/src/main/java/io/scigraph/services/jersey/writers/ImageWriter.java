/**
 * Copyright (C) 2014 The SciGraph authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.scigraph.services.jersey.writers;

import static com.google.common.collect.Lists.newArrayList;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.apache.commons.collections15.Transformer;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.oupls.jung.GraphJung;

import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.algorithms.layout.CircleLayout;
import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.FRLayout2;
import edu.uci.ics.jung.algorithms.layout.ISOMLayout;
import edu.uci.ics.jung.algorithms.layout.KKLayout;
import edu.uci.ics.jung.algorithms.layout.SpringLayout;
import edu.uci.ics.jung.visualization.BasicVisualizationServer;
import io.scigraph.frames.CommonProperties;
import io.scigraph.frames.Concept;
import io.scigraph.frames.NodeProperties;
import io.scigraph.services.jersey.CustomMediaTypes;

@Produces({CustomMediaTypes.IMAGE_JPEG, CustomMediaTypes.IMAGE_PNG})
@Provider
public class ImageWriter extends GraphWriter {

  private static final String DEFAULT_WIDTH = "1024";
  private static final String DEFAULT_HEIGHT = "768";
  private static final String DEFAULT_LAYOUT = "KKLayout";

  @Context
  HttpServletRequest request;

  private static final Transformer<Vertex, String> vertexLabelTransformer = new Transformer<Vertex, String>() {
    @Override
    public String transform(Vertex vertex) {
      String label = "";
      if (vertex.getPropertyKeys().contains(NodeProperties.LABEL)) {
        Object labels = vertex.getProperty(NodeProperties.LABEL);
        if (labels.getClass().isArray()) {
          label = " (" + ((String[])labels)[0] + ")";
        } else {
          label = " (" + vertex.getProperty(NodeProperties.LABEL) + ")";
        }
      }

      // TODO: add curies
      /*if (vertex.getPropertyKeys().contains(CommonProperties.FRAGMENT)) {
        return (String) vertex.getProperty(CommonProperties.FRAGMENT) + label;
      } else {*/
        return (String) vertex.getProperty(CommonProperties.IRI) + label;
      //}
    }
  };

  private static final Transformer<Edge, String> edgeLabelTransformer = new Transformer<Edge, String>() {
    @Override
    public String transform(Edge edge) {
      return edge.getLabel();
    }
  };

  
  private static final Transformer<Vertex, Paint> vertexColorTransformer = new Transformer<Vertex, Paint>() {
    
    List<Color> colors = newArrayList(Color.RED, Color.BLUE, Color.GREEN, Color.GRAY, Color.ORANGE, Color.YELLOW);
    int index = 0;
    
    Map<String, Color> colorMap = new HashMap<>();
    
    @Override
    public Paint transform(Vertex vertex) {
      if (vertex.getPropertyKeys().contains(Concept.CATEGORY)) {
        Object categories = vertex.getProperty(Concept.CATEGORY);
        if (categories.getClass().isArray() || categories instanceof Iterable) {
          return Color.WHITE;
        } else {
          String category = (String) categories;
          if (!colorMap.containsKey(category)) {
            if (index > colors.size()) {
              return Color.WHITE;
            } else {
              colorMap.put(category, colors.get(index++));
            }
          }
          return colorMap.get(category);
        }
      } else {
        return Color.WHITE;
      }
    }
  };

  private static AbstractLayout<Vertex, Edge> getLayout(GraphJung<Graph> graph, String layoutName) {
    switch (layoutName) {
      case "KKLayout":
        return new KKLayout<>(graph);
      case "CircleLayout":
        return new CircleLayout<>(graph);
      case "FRLayout":
        return new FRLayout<>(graph);
      case "FRLayout2":
        return new FRLayout2<>(graph);
      case "ISOMLayout":
        return new ISOMLayout<>(graph);
      case "SpringLayout":
        return new SpringLayout<>(graph);
      default:
        return new KKLayout<>(graph);
    }
  }

  private static BufferedImage renderImage(JPanel panel) {
    JFrame frame = new JFrame();
    frame.setUndecorated(true);
    frame.getContentPane().add(panel);
    frame.pack();
    BufferedImage bi = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
    Graphics2D graphics = bi.createGraphics();
    panel.print(graphics);
    graphics.dispose();
    frame.dispose();
    return bi;
  }

  @Override
  public void writeTo(Graph data, Class<?> type, Type genericType, Annotation[] annotations,
      MediaType mediaType, MultivaluedMap<String, Object> headers, OutputStream out) throws IOException {
    Integer width = Integer.parseInt(Optional.ofNullable(request.getParameter("width")).orElse(DEFAULT_WIDTH));
    Integer height = Integer.parseInt(Optional.ofNullable(request.getParameter("height")).orElse(DEFAULT_HEIGHT));
    String layoutName = Optional.ofNullable(request.getParameter("layout")).orElse(DEFAULT_LAYOUT);

    GraphJung<Graph> graph = new GraphJung<Graph>(data);
    AbstractLayout<Vertex, Edge> layout = getLayout(graph, layoutName);
    layout.setSize(new Dimension(width, height));

    BasicVisualizationServer<Vertex, Edge> viz = new BasicVisualizationServer<>(layout);
    viz.setPreferredSize(new Dimension(width, height));
    viz.getRenderContext().setEdgeLabelTransformer(edgeLabelTransformer);
    viz.getRenderContext().setVertexLabelTransformer(vertexLabelTransformer);
    viz.getRenderContext().setVertexFillPaintTransformer(vertexColorTransformer);

    BufferedImage bi = renderImage(viz);
    String imageType = null;
    if (mediaType.equals(CustomMediaTypes.IMAGE_JPEG_TYPE)) {
      imageType = "jpg";
    } else if (mediaType.equals(CustomMediaTypes.IMAGE_PNG_TYPE)) {
      imageType = "png";
    }
    ImageIO.write(bi, imageType, out);
  }

}

