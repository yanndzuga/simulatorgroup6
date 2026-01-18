package de.frauas.group6.traffic.simulator.view;

import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple, zero-dependency .OBJ file loader for JavaFX.
 * Handles vertices (v), texture coordinates (vt), and faces (f).
 */
public class ObjLoader {

    public static MeshView load(String path) {
        try {
            // 1. Get the file from resources
            InputStream stream = ObjLoader.class.getResourceAsStream(path);
            if (stream == null) {
                System.err.println("CRITICAL: Model file not found at " + path);
                return new MeshView(); // Return empty mesh to prevent crash
            }

            TriangleMesh mesh = new TriangleMesh();
            mesh.setVertexFormat(VertexFormat.POINT_TEXCOORD);

            List<Float> vertices = new ArrayList<>();
            List<Float> texCoords = new ArrayList<>();
            List<Integer> faces = new ArrayList<>();

            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String line;

            // 2. PARSE THE FILE LINE BY LINE
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#") || line.isEmpty()) continue;

                String[] parts = line.split("\\s+");

                switch (parts[0]) {
                    case "v": // Vertex Position: v x y z
                        vertices.add(Float.parseFloat(parts[1]));
                        vertices.add(Float.parseFloat(parts[2]));
                        vertices.add(Float.parseFloat(parts[3]));
                        break;
                    case "vt": // Texture Coordinate: vt u v
                        texCoords.add(Float.parseFloat(parts[1]));
                        // JavaFX texture coordinates are flipped vertically compared to standard OBJ
                        texCoords.add(1f - Float.parseFloat(parts[2])); 
                        break;
                    case "f": // Face: f v1/vt1/vn1 v2/vt2/vn2 ...
                        for (int i = 1; i < parts.length - 1; i++) {
                            // Triangulate faces (convert quads to triangles)
                            parseFaceVertex(parts[1], faces);     // v1
                            parseFaceVertex(parts[i], faces);     // v2
                            parseFaceVertex(parts[i + 1], faces); // v3
                        }
                        break;
                }
            }

            // 3. BUILD THE JAVAFX MESH
            // Convert Lists to primitive arrays
            float[] floatVertices = new float[vertices.size()];
            for (int i = 0; i < vertices.size(); i++) floatVertices[i] = vertices.get(i);

            float[] floatTexCoords;
            if (texCoords.isEmpty()) {
                // If model has no texture mapping, add dummy coordinates
                floatTexCoords = new float[]{0, 0};
            } else {
                floatTexCoords = new float[texCoords.size()];
                for (int i = 0; i < texCoords.size(); i++) floatTexCoords[i] = texCoords.get(i);
            }

            int[] intFaces = new int[faces.size()];
            for (int i = 0; i < faces.size(); i++) intFaces[i] = faces.get(i);

            // Populate the Mesh object
            mesh.getPoints().addAll(floatVertices);
            mesh.getTexCoords().addAll(floatTexCoords);
            mesh.getFaces().addAll(intFaces);

            return new MeshView(mesh);

        } catch (Exception e) {
            e.printStackTrace();
            return new MeshView(); // Return empty on error
        }
    }

    private static void parseFaceVertex(String facePart, List<Integer> faces) {
        // Format is usually v/vt/vn or v//vn or v/vt
        String[] subParts = facePart.split("/");
        
        // OBJ indices start at 1, but Java arrays start at 0. So we subtract 1.
        int vIndex = Integer.parseInt(subParts[0]) - 1;
        faces.add(vIndex);

        int vtIndex = 0;
        if (subParts.length > 1 && !subParts[1].isEmpty()) {
            vtIndex = Integer.parseInt(subParts[1]) - 1;
        }
        faces.add(vtIndex);
    }
}