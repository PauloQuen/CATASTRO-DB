package PROCESOS;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class ReportesAuditoriaPanel extends JPanel {
    // Parámetros para ejecutar la consolidación de auditoría (Transacciones / SP)
    private JTextField txtMuniId, txtAnio, txtZonaId, txtTituloReporte;
    private JButton btnGenerarReporte;

    // Filtros de búsqueda ATÓMICOS para explorar el Maestro existente sin alterar entradas
    private JTextField txtFiltroMuni, txtFiltroAnio;
    private JButton btnLimpiarFiltros;

    // Maestro - Detalle
    private JButton btnArchivarHistorico;
    private JTable tblCabecera, tblDetalle;
    private DefaultTableModel modelCab, modelDet;
    private Connection conexion;

    public ReportesAuditoriaPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Inicializar la conexión real
        conexion = obtenerConexionBD();

        // ==========================================
        // 1) PANEL DE PARÁMETROS SUPERIOR (Escritura / SP)
        // ==========================================
        JPanel pnlFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        pnlFiltros.setBorder(BorderFactory.createTitledBorder(" Criterios de Consolidación de Recaudación (Procesamiento y Congelamiento) "));
        
        txtMuniId = new JTextField(4); 
        txtAnio = new JTextField(4);
        txtZonaId = new JTextField(4); 
        txtTituloReporte = new JTextField(12);
        btnGenerarReporte = new JButton("Calcular y Congelar");

        pnlFiltros.add(new JLabel("Muni ID:")); pnlFiltros.add(txtMuniId);
        pnlFiltros.add(new JLabel("Año:")); pnlFiltros.add(txtAnio);
        pnlFiltros.add(new JLabel("Zona ID (Opt):")); pnlFiltros.add(txtZonaId);
        pnlFiltros.add(new JLabel("Título:")); pnlFiltros.add(txtTituloReporte);
        pnlFiltros.add(btnGenerarReporte);

        // ==========================================
        // 2) COMPONENTE VISUAL MAESTRO-DETALLE + FILTROS DE CONSULTA
        // ==========================================
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerLocation(180); // Ajustado para dar espacio a la barra de búsqueda

        // --- SECCIÓN MAESTRO (Superior) ---
        JPanel pnlM = new JPanel(new BorderLayout(5, 5));
        pnlM.setBorder(BorderFactory.createTitledBorder(" Tabla Maestro (pct_reportes_cab) "));

        // Barra de búsqueda/filtrado exclusiva para la vista de cabeceras (Atomicidad de lectura)
        JPanel pnlBarraBusqueda = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        txtFiltroMuni = new JTextField(4);
        txtFiltroAnio = new JTextField(4);
        btnLimpiarFiltros = new JButton("Ver Todos");

        pnlBarraBusqueda.add(new JLabel("Filtrar por Muni ID:"));
        pnlBarraBusqueda.add(txtFiltroMuni);
        pnlBarraBusqueda.add(new JLabel("Año:"));
        pnlBarraBusqueda.add(txtFiltroAnio);
        pnlBarraBusqueda.add(btnLimpiarFiltros);

        pnlM.add(pnlBarraBusqueda, BorderLayout.NORTH);

        // Tabla Cabecera
        modelCab = new DefaultTableModel(new String[]{"ID Reporte", "Título Cierre", "Fecha Generación", "Muni ID", "Monto Base", "Monto Pagado"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblCabecera = new JTable(modelCab);
        pnlM.add(new JScrollPane(tblCabecera), BorderLayout.CENTER);

        // Botón acción del registro maestro (Mover al Histórico)
        btnArchivarHistorico = new JButton("Mover Reporte Seleccionado al Histórico Físico");
        btnArchivarHistorico.setEnabled(false); // Deshabilitado por seguridad hasta que se seleccione una fila
        JPanel pnlAccionesMaestro = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pnlAccionesMaestro.add(btnArchivarHistorico);
        pnlM.add(pnlAccionesMaestro, BorderLayout.SOUTH);

        // --- SECCIÓN DETALLE (Inferior) ---
        modelDet = new DefaultTableModel(new String[]{"Línea Detail", "Cód. Propietario", "Monto Base Calculado", "Monto Pagado"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblDetalle = new JTable(modelDet);
        JPanel pnlD = new JPanel(new BorderLayout());
        pnlD.setBorder(BorderFactory.createTitledBorder(" Tabla Detalle Analítico (pct_reportes_det) "));
        pnlD.add(new JScrollPane(tblDetalle), BorderLayout.CENTER);

        splitPane.setTopComponent(pnlM);
        splitPane.setBottomComponent(pnlD);

        add(pnlFiltros, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);

        // ==========================================
        // 3) GESTIÓN DE EVENTOS Y LOGICA DE NEGOCIO
        // ==========================================

        // Evento interactivo: Seleccionar Cabecera para cargar su Detalle
        tblCabecera.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int filaSeleccionada = tblCabecera.getSelectedRow();
                if (filaSeleccionada != -1) {
                    int idReporte = Integer.parseInt(tblCabecera.getValueAt(filaSeleccionada, 0).toString());
                    cargarDetalleReporte(idReporte);
                    btnArchivarHistorico.setEnabled(true); // Habilitamos acción al tener una fila válida
                } else {
                    btnArchivarHistorico.setEnabled(false);
                }
            }
        });

        // Generar Reporte de Auditoría (Llama al SP)
        btnGenerarReporte.addActionListener(e -> {
            if (conexion == null) {
                JOptionPane.showMessageDialog(this, "Sin conexión a la base de datos.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (txtMuniId.getText().trim().isEmpty() || txtAnio.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Complete los parámetros obligatorios (Muni ID y Año).", "Faltan Datos", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String sql = "CALL public.sp_generar_reporte_recaudacion(?, ?, ?, ?, ?)";
            try (CallableStatement cstmt = conexion.prepareCall(sql)) {
                cstmt.setInt(1, Integer.parseInt(txtMuniId.getText().trim()));
                cstmt.setShort(2, Short.parseShort(txtAnio.getText().trim()));
                
                if (txtZonaId.getText().trim().isEmpty()) {
                    cstmt.setNull(3, Types.INTEGER);
                } else {
                    cstmt.setInt(3, Integer.parseInt(txtZonaId.getText().trim()));
                }
                
                cstmt.setString(4, txtTituloReporte.getText().trim());
                cstmt.registerOutParameter(5, Types.INTEGER); // OUT: p_repcabcod
                
                cstmt.execute();
                int nuevoIdReporte = cstmt.getInt(5);
                
                JOptionPane.showMessageDialog(this, "¡Reporte #" + nuevoIdReporte + " consolidado y guardado con éxito en la BD!");
                
                // Limpieza transaccional atómica
                txtMuniId.setText(""); txtAnio.setText(""); txtZonaId.setText(""); txtTituloReporte.setText("");
                
                cargarCabecerasExistentes(); // Recargar tabla maestro
                modelDet.setRowCount(0);     // Vaciar detalle anterior
                
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error al ejecutar sp_generar_reporte_recaudacion:\n" + ex.getMessage(), "Error de BD", JOptionPane.ERROR_MESSAGE);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Verifique los formatos numéricos en Muni, Año y Zona.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Archivar Histórico (Llama al SP)
        btnArchivarHistorico.addActionListener(e -> {
            if (conexion == null) return;
            
            int fila = tblCabecera.getSelectedRow();
            if (fila == -1) return; // Control redundante de seguridad
            
            int idReporte = Integer.parseInt(tblCabecera.getValueAt(fila, 0).toString());
            
            int confirmacion = JOptionPane.showConfirmDialog(this, 
                "¿Está seguro de mover permanentemente el reporte #" + idReporte + " al histórico físico?\n" +
                "Esto eliminará los datos de la vista transaccional activa y los migrará a las tablas históricas.", 
                "Confirmar Archivamiento", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                
            if (confirmacion != JOptionPane.YES_OPTION) return;

            String sql = "CALL public.sp_archivar_reporte(?)";
            try (CallableStatement cstmt = conexion.prepareCall(sql)) {
                cstmt.setInt(1, idReporte);
                cstmt.execute();
                
                JOptionPane.showMessageDialog(this, "El reporte #" + idReporte + " ha sido migrado a las tablas históricas (pch_reportes_*) de forma permanente.");
                
                cargarCabecerasExistentes(); // Actualizar cabeceras
                modelDet.setRowCount(0);     // Limpiar detalles
                
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error al archivar reporte:\n" + ex.getMessage(), "Error de BD", JOptionPane.ERROR_MESSAGE);
            }
        });

        // LISTENER DE BUSQUEDA EN TIEMPO REAL (Filtros de Lectura)
        DocumentListener dlFiltros = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { cargarCabecerasExistentes(); }
            @Override public void removeUpdate(DocumentEvent e) { cargarCabecerasExistentes(); }
            @Override public void changedUpdate(DocumentEvent e) { cargarCabecerasExistentes(); }
        };
        txtFiltroMuni.getDocument().addDocumentListener(dlFiltros);
        txtFiltroAnio.getDocument().addDocumentListener(dlFiltros);

        // Acción: Botón limpiar búsqueda
        btnLimpiarFiltros.addActionListener(e -> {
            txtFiltroMuni.setText("");
            txtFiltroAnio.setText(""); // Desencadena automáticamente la recarga completa
        });

        // Carga inicial al desplegar el panel
        cargarCabecerasExistentes();
    }

    // Método de carga adaptado para aplicar filtrado atómico por Muni y Año (Si se ingresan)
    private void cargarCabecerasExistentes() {
        if (conexion == null) return;
        modelCab.setRowCount(0);

        String filtroMuni = txtFiltroMuni.getText().trim();
        String filtroAnio = txtFiltroAnio.getText().trim();

        // Construcción dinámica de la query basada en los campos de búsqueda
        StringBuilder sql = new StringBuilder(
            "SELECT c.repcabcod, p.reptitulo, c.repcabfec, p.repmun, c.repcabtottri, c.repcabtotrec " +
            "FROM public.pct_reportes_cab c " +
            "INNER JOIN public.pcm_reportes_pre p ON c.repcabrep = p.repcod " +
            "WHERE c.repcabestreg = '1' "
        );

        if (!filtroMuni.isEmpty()) {
            sql.append("AND p.repmun = ? ");
        }
        if (!filtroAnio.isEmpty()) {
            sql.append("AND p.repanio = ? ");
        }

        sql.append("ORDER BY c.repcabcod DESC");

        try (PreparedStatement pstmt = conexion.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            if (!filtroMuni.isEmpty()) {
                pstmt.setInt(paramIndex++, Integer.parseInt(filtroMuni));
            }
            if (!filtroAnio.isEmpty()) {
                pstmt.setShort(paramIndex++, Short.parseShort(filtroAnio));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    modelCab.addRow(new Object[]{
                        rs.getInt("repcabcod"),
                        rs.getString("reptitulo"),
                        rs.getDate("repcabfec"),
                        rs.getInt("repmun"),
                        "S/. " + rs.getBigDecimal("repcabtottri"),
                        "S/. " + rs.getBigDecimal("repcabtotrec")
                    });
                }
            }
        } catch (NumberFormatException nfe) {
            // Silenciar errores en caso de que escriban caracteres no válidos mientras buscan
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar reportes consolidados: " + ex.getMessage(), "Error de Consulta", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cargarDetalleReporte(int idReporte) {
        if (conexion == null) return;
        modelDet.setRowCount(0);
        
        String sql = "SELECT repdetcod, repdetprop, repdetmoncal, repdetmonpag " +
                     "FROM public.pct_reportes_det WHERE repdetcab = ? ORDER BY repdetcod ASC";
                     
        try (PreparedStatement pstmt = conexion.prepareStatement(sql)) {
            pstmt.setInt(1, idReporte);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    modelDet.addRow(new Object[]{
                        rs.getInt("repdetcod"),
                        rs.getInt("repdetprop"),
                        "S/. " + rs.getBigDecimal("repdetmoncal"),
                        "S/. " + rs.getBigDecimal("repdetmonpag")
                    });
                }
            }
        } catch (SQLException ex) {
            System.out.println("Error cargando tabla de detalles analíticos: " + ex.getMessage());
        }
    }

    private Connection obtenerConexionBD() {
        try {
            Class.forName("org.postgresql.Driver");
            String url = "jdbc:postgresql://localhost:5432/catastro_municipal";
            String usuario = "postgres";
            String password = "pauloq3408";
            return DriverManager.getConnection(url, usuario, password);
        } catch (Exception e) {
            System.out.println("Error de conexión en ReportesAuditoriaPanel: " + e.getMessage());
            return null;
        }
    }
}