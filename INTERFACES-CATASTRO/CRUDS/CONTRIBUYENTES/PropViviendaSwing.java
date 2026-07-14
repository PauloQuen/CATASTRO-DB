package CRUDS.CONTRIBUYENTES;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class PropViviendaSwing extends JPanel {

    static final String URL = "jdbc:postgresql://localhost:5432/catastro_municipal";
    static final String USER = "postgres";
    static final String PASSWORD = "pauloq3408"; 
    static final String DRIVER = "org.postgresql.Driver";

    // Componentes del Formulario
    private JTextField txtPvPrp, txtPvViv, txtPvTitulo;
    private JTable tablaDatos;
    private DefaultTableModel modeloTabla;
    
    private boolean isAjustando = false; 
    private Container principalContainer;
    private String vistaOrigen;

    public PropViviendaSwing(Container container, String vistaOrigen) {
        this.principalContainer = container;
        this.vistaOrigen = vistaOrigen;

        // Configuración del Layout del Panel propio
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        // 0. CONTENEDOR SUPERIOR PARA EL BOTÓN DE REGRESO
        JPanel panelNavegacion = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
        JButton btnVolver = new JButton("← Volver al Menú de Contribuyentes");
        btnVolver.setBackground(new Color(245, 245, 245));
        panelNavegacion.add(btnVolver);

        // 1. FORMULARIO DE ENTRADA
        JPanel panelFormulario = new JPanel(new GridLayout(2, 4, 8, 8));
        panelFormulario.setBorder(BorderFactory.createTitledBorder(" Datos de la Asignación Dominial (PostgreSQL) "));

        txtPvPrp = new JTextField();
        txtPvViv = new JTextField();       
        txtPvTitulo = new JTextField();       

        panelFormulario.add(new JLabel("Código Propietario (provivprp):")); panelFormulario.add(txtPvPrp);
        panelFormulario.add(new JLabel("Código Vivienda (provivviv):")); panelFormulario.add(txtPvViv);
        panelFormulario.add(new JLabel("Título de Propiedad (provivtitulo):")); panelFormulario.add(txtPvTitulo);
        panelFormulario.add(new JLabel("")); panelFormulario.add(new JLabel(""));

        // 2. BOTONES DE CONTROL
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JButton btnInsertar = new JButton("Asignar Propiedad");
        JButton btnActualizar = new JButton("Modificar Título");
        JButton btnEliminar = new JButton("Desvincular (Físico)");
        JButton btnLimpiar = new JButton("Limpiar Formulario");

        btnEliminar.setBackground(new Color(255, 200, 200));

        panelBotones.add(btnInsertar);
        panelBotones.add(btnActualizar);
        panelBotones.add(btnEliminar);
        panelBotones.add(btnLimpiar);

        // Estructura del Panel Norte
        JPanel panelNorte = new JPanel(new BorderLayout(5, 5));
        panelNorte.add(panelNavegacion, BorderLayout.NORTH);
        panelNorte.add(panelFormulario, BorderLayout.CENTER);
        panelNorte.add(panelBotones, BorderLayout.SOUTH);
        add(panelNorte, BorderLayout.NORTH);

        // 3. TABLA DE CONTROL CON MAPEO ACTUALIZADO
        String[] columnas = {
            "ID Propietario", "Nombre/Ciudadano", "Código Predio", "Ubicación (Vía / Nro)", "Título Propiedad", "Estado Reg"
        };
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaDatos = new JTable(modeloTabla);
        JScrollPane scrollTabla = new JScrollPane(tablaDatos);
        add(scrollTabla, BorderLayout.CENTER);

        // OPERACIÓN READ - Campos actualizados a minúsculas exactos del nuevo .sql
        Runnable cargarDatos = () -> {
            isAjustando = true; 
            modeloTabla.setRowCount(0);
            
            String sql = "SELECT pv.provivprp, p.pernom, p.perapepat, pv.provivviv, d.dirvianom, d.dirnum, pv.provivtitulo, pv.provivestreg "
                       + "FROM h8m_prop_vivienda pv "
                       + "INNER JOIN h8m_propietario pr ON pv.provivprp = pr.procod "
                       + "INNER JOIN h6m_persona p ON pr.proper = p.perdni "
                       + "INNER JOIN c3m_vivienda v ON pv.provivviv = v.vivcod "
                       + "INNER JOIN c3m_direccion d ON v.vivdir = d.dircod "
                       + "WHERE pv.provivestreg = '1' "
                       + "ORDER BY pv.provivprp ASC, pv.provivviv ASC";
            
            try (Connection conn = conectar();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                while (rs.next()) {
                    Vector<Object> fila = new Vector<>();
                    fila.add(rs.getInt("provivprp"));
                    
                    String nombreCompleto = rs.getString("perapepat").trim() + ", " + rs.getString("pernom").trim();
                    fila.add(nombreCompleto);
                    fila.add(rs.getString("provivviv").trim());
                    fila.add(rs.getString("dirvianom").trim() + " #" + rs.getString("dirnum").trim());
                    
                    Object titulo = rs.getObject("provivtitulo");
                    fila.add(titulo != null ? titulo.toString().trim() : "");
                    fila.add(rs.getString("provivestreg").trim());
                    modeloTabla.addRow(fila);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al mapear registros dominiales:\n" + ex.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
            } finally {
                isAjustando = false; 
            }
        };

        // Carga diferida inicial
        Timer timer = new Timer(200, e -> cargarDatos.run());
        timer.setRepeats(false);
        timer.start();

        // ACCIÓN: MAPEADO DE SELECCIÓN
        tablaDatos.getSelectionModel().addListSelectionListener(e -> {
            if (isAjustando || e.getValueIsAdjusting()) return;
            
            int fila = tablaDatos.getSelectedRow();
            if (fila != -1) {
                isAjustando = true;
                String proCodSel = modeloTabla.getValueAt(fila, 0).toString();
                String vivCodSel = modeloTabla.getValueAt(fila, 2).toString();
                Object tituloObj = modeloTabla.getValueAt(fila, 4);
                String tituloSel = (tituloObj != null) ? tituloObj.toString() : "";

                txtPvPrp.setText(proCodSel);
                txtPvViv.setText(vivCodSel);
                txtPvTitulo.setText(tituloSel);

                txtPvPrp.setEditable(false);
                txtPvViv.setEditable(false);
                isAjustando = false;
            }
        });

        // OPERACIÓN CREATE: ASIGNAR PROPIEDAD
        btnInsertar.addActionListener(e -> {
            String strPvPrp = txtPvPrp.getText().trim();
            String strPvViv = txtPvViv.getText().trim();
            String strPvTitulo = txtPvTitulo.getText().trim();

            if (strPvPrp.isEmpty() || strPvViv.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Los campos de la clave (Propietario y Vivienda) son mandatorios.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try (Connection conn = conectar()) {
                int proCod = Integer.parseInt(strPvPrp);

                // Validación contra nombres reales de tablas y columnas (en minúsculas)
                if (!existeId(conn, "h8m_propietario", "procod", proCod)) {
                    JOptionPane.showMessageDialog(this, "El código de propietario no existe en h8m_propietario.", "Violación FK", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (!existeId(conn, "c3m_vivienda", "vivcod", strPvViv)) {
                    JOptionPane.showMessageDialog(this, "El código de la vivienda no existe en c3m_vivienda.", "Violación FK", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (existeRelacion(conn, proCod, strPvViv)) {
                    JOptionPane.showMessageDialog(this, "Vínculo existente. Use 'Modificar Título' si desea alterar el documento.", "Clave Duplicada", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // Ajustado a provivprp, provivviv, provivfecini, provivtitulo, provivestreg
                String sqlInsert = "INSERT INTO h8m_prop_vivienda (provivprp, provivviv, provivfecini, provivtitulo, provivestreg) VALUES (?, ?, CURRENT_DATE, ?, '1')";
                try (PreparedStatement stmt = conn.prepareStatement(sqlInsert)) {
                    stmt.setInt(1, proCod);
                    stmt.setString(2, strPvViv);
                    if (strPvTitulo.isEmpty()) stmt.setNull(3, Types.VARCHAR);
                    else stmt.setString(3, strPvTitulo);
                    stmt.executeUpdate();
                }

                JOptionPane.showMessageDialog(this, "¡Relación catastral guardada exitosamente!");
                cargarDatos.run();
                btnLimpiar.doClick();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "El código de propietario debe ser un número entero válido.", "Error de Tipado", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error de Persistencia:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // OPERACIÓN UPDATE: MODIFICAR TÍTULO
        btnActualizar.addActionListener(e -> {
            String strPvPrp = txtPvPrp.getText().trim();
            String strPvViv = txtPvViv.getText().trim();
            String strPvTitulo = txtPvTitulo.getText().trim();

            if (strPvPrp.isEmpty() || strPvViv.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Seleccione un registro o especifique las llaves primarias.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try (Connection conn = conectar()) {
                int proCod = Integer.parseInt(strPvPrp);

                if (!existeRelacion(conn, proCod, strPvViv)) {
                    JOptionPane.showMessageDialog(this, "No se encontró ningún registro que coincida con las claves indicadas.", "Error de Referencia", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                String sqlUpdate = "UPDATE h8m_prop_vivienda SET provivtitulo = ? WHERE provivprp = ? AND provivviv = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sqlUpdate)) {
                    if (strPvTitulo.isEmpty()) stmt.setNull(1, Types.VARCHAR);
                    else stmt.setString(1, strPvTitulo);
                    stmt.setInt(2, proCod);
                    stmt.setString(3, strPvViv);
                    stmt.executeUpdate();
                }

                JOptionPane.showMessageDialog(this, "¡Título de propiedad actualizado!");
                cargarDatos.run();
                btnLimpiar.doClick();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al actualizar:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // OPERACIÓN DELETE: ELIMINACIÓN FÍSICA
        btnEliminar.addActionListener(e -> {
            String strPvPrp = txtPvPrp.getText().trim();
            String strPvViv = txtPvViv.getText().trim();

            if (strPvPrp.isEmpty() || strPvViv.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Defina la combinación exacta de claves para proceder.", "Campos Mandatorios", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int confirmar = JOptionPane.showConfirmDialog(this, 
                "¿Está seguro de eliminar físicamente esta asignación de la base catastral?", 
                "Confirmación de Purga Física", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
            if (confirmar != JOptionPane.YES_OPTION) return;

            try (Connection conn = conectar()) {
                int proCod = Integer.parseInt(strPvPrp);

                String sqlDelete = "DELETE FROM h8m_prop_vivienda WHERE provivprp = ? AND provivviv = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sqlDelete)) {
                    stmt.setInt(1, proCod);
                    stmt.setString(2, strPvViv);
                    
                    int rows = stmt.executeUpdate();
                    if (rows > 0) {
                        JOptionPane.showMessageDialog(this, "Vínculo removido permanentemente de la base de datos.");
                    } else {
                        JOptionPane.showMessageDialog(this, "No se halló la relación solicitada.", "Sin cambios", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
                cargarDatos.run();
                btnLimpiar.doClick();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al eliminar:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // ACCIÓN: LIMPIAR FORMULARIO
        btnLimpiar.addActionListener(e -> {
            isAjustando = true;
            txtPvPrp.setText("");
            txtPvViv.setText("");
            txtPvTitulo.setText("");
            txtPvPrp.setEditable(true);
            txtPvViv.setEditable(true);
            tablaDatos.clearSelection();
            isAjustando = false;
        });

        // ACCIÓN: VOLVER
        btnVolver.addActionListener(e -> {
            if (principalContainer != null && principalContainer.getLayout() instanceof CardLayout) {
                btnLimpiar.doClick(); 
                CardLayout cl = (CardLayout) principalContainer.getLayout();
                cl.show(principalContainer, vistaOrigen); 
            } else {
                Component topAncestor = SwingUtilities.getWindowAncestor(this);
                if (topAncestor instanceof JFrame) {
                    ((JFrame) topAncestor).dispose();
                }
            }
        });
    }

    // Métodos Auxiliares
    private boolean existeId(Connection conn, String tabla, String campoId, Object id) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + tabla + " WHERE " + campoId + " = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (id instanceof Integer) {
                stmt.setInt(1, (Integer) id);
            } else {
                stmt.setString(1, id.toString().trim());
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    private boolean existeRelacion(Connection conn, int proCod, String vivCod) throws SQLException {
        String sql = "SELECT COUNT(*) FROM h8m_prop_vivienda WHERE provivprp = ? AND provivviv = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, proCod);
            stmt.setString(2, vivCod);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    private Connection conectar() throws SQLException {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            System.out.println("Nota: Cargando driver implícitamente mediante DriverManager.");
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}