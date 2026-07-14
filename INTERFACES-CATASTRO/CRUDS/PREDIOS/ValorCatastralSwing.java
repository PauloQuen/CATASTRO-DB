package CRUDS.PREDIOS;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.Vector;

public class ValorCatastralSwing extends JPanel {

    static final String URL = "jdbc:postgresql://localhost:5432/catastro_municipal";
    static final String USER = "postgres";
    static final String PASSWORD = "pauloq3408"; 
    static final String DRIVER = "org.postgresql.Driver";

    // Componentes de la interfaz (Campos en minúsculas basados en PostgreSQL)
    private JTextField txtValCod, txtValViv, txtValAnio, txtValMon;
    private JTable tablaDatos;
    private DefaultTableModel modeloTabla;
    
    private boolean isAjustando = false; 

    public ValorCatastralSwing(JPanel contenedorPadre, String destinoRetorno) {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        // --- BOTÓN VOLVER (Integración modular) ---
        JButton btnVolver = new JButton("← Volver al Panel de Selección");
        btnVolver.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnVolver.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnVolver.addActionListener(e -> {
            CardLayout layout = (CardLayout) contenedorPadre.getLayout();
            layout.show(contenedorPadre, destinoRetorno);
        });
        
        JPanel pnlSuperiorWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlSuperiorWrapper.add(btnVolver);

        // 1. PANEL DE FORMULARIO
        JPanel panelFormulario = new JPanel(new GridLayout(2, 4, 8, 8));
        panelFormulario.setBorder(BorderFactory.createTitledBorder(" Datos de Valoración Anual Catastral (c5m_valor_catastral) "));

        txtValCod = new JTextField();
        txtValCod.setEditable(false);
        txtValCod.setBackground(new Color(240, 240, 240)); // Serial, se genera solo en el INSERT
        
        txtValViv = new JTextField();      
        txtValAnio = new JTextField();      
        txtValMon = new JTextField();      

        panelFormulario.add(new JLabel("Código Valoración (valcod - Auto):")); panelFormulario.add(txtValCod);
        panelFormulario.add(new JLabel("Código Predio/Vivienda (valviv):")); panelFormulario.add(txtValViv);
        panelFormulario.add(new JLabel("Año Fiscal (valanio):")); panelFormulario.add(txtValAnio);
        panelFormulario.add(new JLabel("Monto Autovalúo S/ (valmon):")); panelFormulario.add(txtValMon);

        // 2. CONTROLADORES (BOTONES)
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JButton btnInsertar = new JButton("Registrar Tasación");
        JButton btnActualizar = new JButton("Rectificar Monto");
        JButton btnEliminar = new JButton("Dar de Baja");
        JButton btnLimpiar = new JButton("Limpiar Formulario");

        btnEliminar.setBackground(new Color(255, 190, 190));

        panelBotones.add(btnInsertar);
        panelBotones.add(btnActualizar);
        panelBotones.add(btnEliminar);
        panelBotones.add(btnLimpiar);

        JPanel panelNorte = new JPanel(new BorderLayout(5, 5));
        panelNorte.add(pnlSuperiorWrapper, BorderLayout.NORTH);
        panelNorte.add(panelFormulario, BorderLayout.CENTER);
        panelNorte.add(panelBotones, BorderLayout.SOUTH);
        add(panelNorte, BorderLayout.NORTH);

        // 3. TABLA DE REGISTROS ECONÓMICOS
        String[] columnas = {
            "Código Valor", "Cod. Predio", "Ubicación Urbana", "Año Fiscal", "Valor Autovalúo (S/)", "Fec. Registro", "Fec. Modificación"
        };
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaDatos = new JTable(modeloTabla);
        tablaDatos.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        add(new JScrollPane(tablaDatos), BorderLayout.CENTER);

        // OPERACIÓN READ (Mapeado con sintaxis e inner joins de PostgreSQL)
        Runnable cargarDatos = () -> {
            isAjustando = true; 
            modeloTabla.setRowCount(0);
            
            String sql = "SELECT vc.valcod, vc.valviv, vc.valanio, vc.valmon, vc.valfecreg, vc.valfecmod, d.dirvianom, d.dirnum "
                       + "FROM c5m_valor_catastral vc "
                       + "INNER JOIN c3m_vivienda v ON vc.valviv = v.vivcod "
                       + "INNER JOIN c3m_direccion d ON v.vivdir = d.dircod "
                       + "WHERE vc.valestreg = '1' "
                       + "ORDER BY vc.valanio DESC, vc.valcod ASC";
            
            try (Connection conn = conectar();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                while (rs.next()) {
                    Vector<Object> fila = new Vector<>();
                    fila.add(rs.getInt("valcod"));
                    fila.add(rs.getString("valviv"));
                    fila.add(rs.getString("dirvianom") + " #" + rs.getInt("dirnum"));
                    fila.add(rs.getShort("valanio"));
                    fila.add(rs.getBigDecimal("valmon"));
                    fila.add(rs.getDate("valfecreg"));
                    Date fecMod = rs.getDate("valfecmod");
                    fila.add(fecMod != null ? fecMod : "Sin Modificaciones");
                    modeloTabla.addRow(fila);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al listar el historial económico:\n" + ex.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
            } finally {
                isAjustando = false; 
            }
        };

        cargarDatos.run();

        // ACCIÓN DE SELECCIÓN DE FILAS
        tablaDatos.getSelectionModel().addListSelectionListener(e -> {
            if (isAjustando || e.getValueIsAdjusting()) return;
            
            int fila = tablaDatos.getSelectedRow();
            if (fila != -1) {
                isAjustando = true;
                txtValCod.setText(modeloTabla.getValueAt(fila, 0).toString());
                txtValViv.setText(modeloTabla.getValueAt(fila, 1).toString());
                txtValAnio.setText(modeloTabla.getValueAt(fila, 3).toString());
                txtValMon.setText(modeloTabla.getValueAt(fila, 4).toString());
                isAjustando = false;
            }
        });

        // OPERACIÓN CREATE: REGISTRAR TASACIÓN ANUAL (valcod omitido por ser SERIAL)
        btnInsertar.addActionListener(e -> {
            String strValViv = txtValViv.getText().trim();
            String strValAnio = txtValAnio.getText().trim();
            String strValMon = txtValMon.getText().trim();

            if (strValViv.isEmpty() || strValAnio.isEmpty() || strValMon.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Código de Vivienda, Año Fiscal y Monto son obligatorios.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try (Connection conn = conectar()) {
                short valAnio = Short.parseShort(strValAnio);
                BigDecimal valMon = new BigDecimal(strValMon);

                // Validar FK Real de Vivienda (vivcod en c3m_vivienda)
                if (!existeId(conn, "c3m_vivienda", "vivcod", strValViv)) {
                    JOptionPane.showMessageDialog(this, "El código de predio '" + strValViv + "' no existe en el sistema catastral.", "Violación FK", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Inserción limpia usando CURRENT_DATE nativo de PostgreSQL
                String sqlInsert = "INSERT INTO c5m_valor_catastral (valviv, valanio, valmon, valfecreg, valfecmod, valestreg) VALUES (?, ?, ?, CURRENT_DATE, NULL, '1')";
                try (PreparedStatement stmt = conn.prepareStatement(sqlInsert)) {
                    stmt.setString(1, strValViv);
                    stmt.setShort(2, valAnio);
                    stmt.setBigDecimal(3, valMon);
                    stmt.executeUpdate();
                }

                JOptionPane.showMessageDialog(this, "✔ Tasación catastral registrada correctamente para el año " + valAnio + "!");
                cargarDatos.run();
                btnLimpiar.doClick();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Error de Formato:\n- El Año debe ser un entero corto.\n- El Monto debe ser un valor decimal válido.", "Error de Tipos", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error de Persistencia:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // OPERACIÓN UPDATE: RECTIFICAR MONTO DE AUTOVALÚO Y CONFIGURAR FECHA DE MODIFICACIÓN
        btnActualizar.addActionListener(e -> {
            String strValCod = txtValCod.getText().trim();
            String strValMon = txtValMon.getText().trim();

            if (strValCod.isEmpty() || strValMon.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Seleccione un registro en la tabla para rectificar el monto.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try (Connection conn = conectar()) {
                int valCod = Integer.parseInt(strValCod);
                BigDecimal nuevoMonto = new BigDecimal(strValMon);

                String sqlUpdate = "UPDATE c5m_valor_catastral SET valmon = ?, valfecmod = CURRENT_DATE WHERE valcod = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sqlUpdate)) {
                    stmt.setBigDecimal(1, nuevoMonto);
                    stmt.setInt(2, valCod);
                    stmt.executeUpdate();
                }

                JOptionPane.showMessageDialog(this, "✔ Monto de autovalúo rectificado correctamente en la ficha.");
                cargarDatos.run();
                btnLimpiar.doClick();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "El monto ingresado debe ser un valor monetario decimal.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al actualizar:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // OPERACIÓN DELETE: BAJA LÓGICA (valestreg = '0')
        btnEliminar.addActionListener(e -> {
            String strValCod = txtValCod.getText().trim();

            if (strValCod.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Seleccione la tasación que desea dar de baja administrativa.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int confirmar = JOptionPane.showConfirmDialog(this, 
                "¿Está seguro de dar de baja lógicamente este registro histórico económico (valestreg = '0')?", 
                "Confirmación Administrativa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
            if (confirmar != JOptionPane.YES_OPTION) return;

            try (Connection conn = conectar()) {
                int valCod = Integer.parseInt(strValCod);

                String sqlDelete = "UPDATE c5m_valor_catastral SET valestreg = '0' WHERE valcod = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sqlDelete)) {
                    stmt.setInt(1, valCod);
                    stmt.executeUpdate();
                }
                JOptionPane.showMessageDialog(this, "Registro histórico dado de baja del sistema activo.");
                cargarDatos.run();
                btnLimpiar.doClick();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al procesar la baja:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // REINICIAR FORMULARIO
        btnLimpiar.addActionListener(e -> {
            isAjustando = true;
            txtValCod.setText("");
            txtValViv.setText("");
            txtValAnio.setText("");
            txtValMon.setText("");
            tablaDatos.clearSelection();
            isAjustando = false;
        });
    }

    // Método Auxiliar de Validación genérico para PostgreSQL
    private boolean existeId(Connection conn, String tabla, String campoId, Object id) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + tabla + " WHERE " + campoId + " = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (id instanceof Integer) stmt.setInt(1, (Integer) id);
            else if (id instanceof Short) stmt.setShort(1, (Short) id);
            else stmt.setString(1, (String) id);
            
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
            throw new SQLException("Driver JDBC de PostgreSQL no localizado.");
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}