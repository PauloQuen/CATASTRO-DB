package VIEWS.SISTEMA_TRIBUTARIO;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class VwRecaudacionVsPresupuestoSwing extends JPanel {
    private DefaultTableModel modelo = new DefaultTableModel() {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    
    private TableRowSorter<DefaultTableModel> sorter;
    private JTextField txtBuscar;

    public VwRecaudacionVsPresupuestoSwing(JPanel contenedorPadre, String claveRetorno) {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // --- PANEL NORTE: Control de navegación y Buscador ---
        JPanel pnlNorte = new JPanel(new BorderLayout(10, 10));
        
        JButton btnVolver = new JButton("← Volver al Menú Tributario");
        btnVolver.addActionListener(e -> ((CardLayout)contenedorPadre.getLayout()).show(contenedorPadre, claveRetorno));
        pnlNorte.add(btnVolver, BorderLayout.WEST);

        // Subpanel para la barra de búsqueda
        JPanel pnlBuscar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pnlBuscar.add(new JLabel("Buscar Municipalidad/Año: "));
        txtBuscar = new JTextField(20);
        pnlBuscar.add(txtBuscar);
        pnlNorte.add(pnlBuscar, BorderLayout.EAST);
        
        add(pnlNorte, BorderLayout.NORTH);

        // --- PANEL CENTRO: Tabla con datos ---
        JTable tabla = new JTable(modelo);
        sorter = new TableRowSorter<>(modelo);
        tabla.setRowSorter(sorter);
        
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        // --- LÓGICA DEL FILTRO (Escucha cambios de texto) ---
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filtrar(); }
            @Override public void removeUpdate(DocumentEvent e) { filtrar(); }
            @Override public void changedUpdate(DocumentEvent e) { filtrar(); }
        });

        cargarDatos();
    }

    private void filtrar() {
        String texto = txtBuscar.getText();
        if (texto.trim().length() == 0) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + texto));
        }
    }

    private void cargarDatos() {
        modelo.setRowCount(0);
        modelo.setColumnCount(0);

        String url = "jdbc:postgresql://localhost:5432/catastro_municipal";
        String sql = "SELECT * FROM public.vw_recaudacion_vs_presupuesto"; 

        try (Connection conn = DriverManager.getConnection(url, "postgres", "pauloq3408");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData meta = rs.getMetaData();
            int columnas = meta.getColumnCount();

            for (int i = 1; i <= columnas; i++) {
                modelo.addColumn(meta.getColumnLabel(i));
            }

            while (rs.next()) {
                Vector<Object> fila = new Vector<>();
                for (int i = 1; i <= columnas; i++) {
                    fila.add(rs.getObject(i));
                }
                modelo.addRow(fila);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar control presupuestal: " + ex.getMessage());
        }
    }
}