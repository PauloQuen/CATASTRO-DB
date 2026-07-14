package VIEWS.SISTEMA_TRIBUTARIO;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class ReportesFinancierosSwing extends JPanel {

    private final JPanel contenedorPadre;
    private final String destinoRetorno;
    
    // Componentes de interfaz
    private JTextField txtMuniId, txtAnio, txtZonaId, txtTitulo;
    private JTable tblCabeceras, tblDetalles;
    private DefaultTableModel modeloCabecera, modeloDetalle;
    
    // Reemplaza esto con tu clase de conexión real (ej. ConexionDB.getConexion())
    private Connection conexion; 

    public ReportesFinancierosSwing(JPanel contenedorPadre, String destinoRetorno) {
        this.contenedorPadre = contenedorPadre;
        this.destinoRetorno = destinoRetorno;
        this.conexion = obtenerConexionBD(); // Inicializa tu conexión aquí
        
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        initComponentes();
        cargarCabecerasExistentes(); // Carga los reportes que ya estén guardados al abrir la pantalla
    }

    private void initComponentes() {
        // 1. FILTROS Y PARÁMETROS DE EMISIÓN
        JPanel pnlFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 5));
        pnlFiltros.setBorder(BorderFactory.createTitledBorder(" Parámetros de Auditoría Financiera "));
        
        pnlFiltros.add(new JLabel("Muni ID:"));
        txtMuniId = new JTextField(4); pnlFiltros.add(txtMuniId);
        
        pnlFiltros.add(new JLabel("Año:"));
        txtAnio = new JTextField(4); pnlFiltros.add(txtAnio);
        
        pnlFiltros.add(new JLabel("Zona ID (Opcional):"));
        txtZonaId = new JTextField(4); pnlFiltros.add(txtZonaId);
        
        pnlFiltros.add(new JLabel("Título:"));
        txtTitulo = new JTextField(12); pnlFiltros.add(txtTitulo);
        
        JButton btnEjecutar = new JButton("Generar y Congelar Reporte");
        pnlFiltros.add(btnEjecutar);
        add(pnlFiltros, BorderLayout.NORTH);

        // 2. CONFIGURACIÓN DE TABLAS MAESTRO-DETALLE
        JSplitPane splitPaneTablas = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPaneTablas.setDividerLocation(150);

        // Tabla Maestro (Cabeceras)
        String[] columnasCab = {"ID Reporte", "Título", "Fecha Generación", "Total Calculado", "Total Recaudado"};
        modeloCabecera = new DefaultTableModel(columnasCab, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblCabeceras = new JTable(modeloCabecera);
        JPanel pnlContenedorCab = new JPanel(new BorderLayout());
        pnlContenedorCab.setBorder(BorderFactory.createTitledBorder(" Reportes Generados Guardados (pct_reportes_cab) "));
        pnlContenedorCab.add(new JScrollPane(tblCabeceras), BorderLayout.CENTER);

        JButton btnArchivar = new JButton("Archivar Reporte Seleccionado");
        JPanel pnlBotoneraMaestro = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pnlBotoneraMaestro.add(btnArchivar);
        pnlContenedorCab.add(pnlBotoneraMaestro, BorderLayout.SOUTH);

        // Tabla Detalle
        String[] columnasDet = {"Línea", "Cód. Propietario", "Monto Tributo", "Monto Recaudado", "Estado"};
        modeloDetalle = new DefaultTableModel(columnasDet, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblDetalles = new JTable(modeloDetalle);
        JPanel pnlContenedorDet = new JPanel(new BorderLayout());
        pnlContenedorDet.setBorder(BorderFactory.createTitledBorder(" Desglose de Contribuyentes Auditados (pct_reportes_det) "));
        pnlContenedorDet.add(new JScrollPane(tblDetalles), BorderLayout.CENTER);

        splitPaneTablas.setTopComponent(pnlContenedorCab);
        splitPaneTablas.setBottomComponent(pnlContenedorDet);
        add(splitPaneTablas, BorderLayout.CENTER);

        // ==============================================================================
        // LOGICA DE EVENTOS (AQUÍ ESTÁ LA MAGIA QUE FALTA)
        // ==============================================================================

        // Evento 1: Al hacer click en una fila de la tabla superior, cargar su detalle abajo
        tblCabeceras.getSelectionModel().addListSelectionListener((ListSelectionEvent e) -> {
            if (!e.getValueIsAdjusting()) {
                int filaSeleccionada = tblCabeceras.getSelectedRow();
                if (filaSeleccionada != -1) {
                    // Obtener el ID del reporte de la columna 0
                    int idReporte = Integer.parseInt(tblCabeceras.getValueAt(filaSeleccionada, 0).toString());
                    cargarDetalleReporte(idReporte);
                }
            }
        });

        // Evento 2: Botón para ejecutar el procedimiento almacenado en la BD
        btnEjecutar.addActionListener(e -> {
            try {
                int muniId = Integer.parseInt(txtMuniId.getText().trim());
                short anio = Short.parseShort(txtAnio.getText().trim());
                String titulo = txtTitulo.getText().trim();
                
                String sql = "{call public.sp_generar_reporte_recaudacion(?, ?, ?, ?, ?)}";
                try (CallableStatement cstmt = conexion.prepareCall(sql)) {
                    cstmt.setInt(1, muniId);
                    cstmt.setShort(2, anio);
                    
                    if (txtZonaId.getText().trim().isEmpty()) {
                        cstmt.setNull(3, Types.INTEGER);
                    } else {
                        cstmt.setInt(3, Integer.parseInt(txtZonaId.getText().trim()));
                    }
                    
                    cstmt.setString(4, titulo);
                    cstmt.registerOutParameter(5, Types.INTEGER); // OUT p_repcabcod
                    
                    cstmt.execute();
                    int nuevoIdReporte = cstmt.getInt(5);
                    
                    JOptionPane.showMessageDialog(this, "¡Reporte #" + nuevoIdReporte + " consolidado en la BD!");
                    cargarCabecerasExistentes(); // Refrescar tabla de arriba
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al generar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Evento 3: Botón para archivar (Historiar)
        btnArchivar.addActionListener(e -> {
            int fila = tblCabeceras.getSelectedRow();
            if (fila == -1) {
                JOptionPane.showMessageDialog(this, "Seleccione un reporte de la lista de arriba.");
                return;
            }
            int idReporte = Integer.parseInt(tblCabeceras.getValueAt(fila, 0).toString());
            
            try (CallableStatement cstmt = conexion.prepareCall("{call public.sp_archivar_reporte(?)}")) {
                cstmt.setInt(1, idReporte);
                cstmt.execute();
                JOptionPane.showMessageDialog(this, "Reporte movido al histórico correctamente.");
                cargarCabecerasExistentes();
                modeloDetalle.setRowCount(0); // Limpiar detalle vacío
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error al archivar: " + ex.getMessage());
            }
        });
    }

    // Método para llenar la tabla de arriba desde la Base de Datos
    private void cargarCabecerasExistentes() {
        if (conexion == null) return;
        modeloCabecera.setRowCount(0);
        String sql = "SELECT repcabcod, reptitulo, repcabfec, repcabtottri, repcabtotrec FROM public.pct_reportes_cab ORDER BY repcabcod DESC";
        try (Statement stmt = conexion.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                modeloCabecera.addRow(new Object[]{
                    rs.getInt("repcabcod"),
                    rs.getString("reptitulo"),
                    rs.getDate("repcabfec"),
                    "S/. " + rs.getBigDecimal("repcabtottri"),
                    "S/. " + rs.getBigDecimal("repcabtotrec")
                });
            }
        } catch (SQLException ex) {
            System.out.println("Error cargando cabeceras: " + ex.getMessage());
        }
    }

    // Método para llenar la tabla de abajo según la cabecera seleccionada
    private void cargarDetalleReporte(int idReporte) {
        if (conexion == null) return;
        modeloDetalle.setRowCount(0);
        String sql = "SELECT repdetlin, h8m_propietariocod, repdettributo, repdetrecaudado, repdetestado FROM public.pct_reportes_det WHERE repcabcod = ? ORDER BY repdetlin ASC";
        try (PreparedStatement pstmt = conexion.prepareStatement(sql)) {
            pstmt.setInt(1, idReporte);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    modeloDetalle.addRow(new Object[]{
                        rs.getInt("repdetlin"),
                        rs.getInt("h8m_propietariocod"),
                        "S/. " + rs.getBigDecimal("repdettributo"),
                        "S/. " + rs.getBigDecimal("repdetrecaudado"),
                        rs.getString("repdetestado")
                    });
                }
            }
        } catch (SQLException ex) {
            System.out.println("Error cargando detalles: " + ex.getMessage());
        }
    }

    private Connection obtenerConexionBD() {
        // CAMBIA ESTO CON TUS CREDENCIALES REALES DE POSTGRESQL PARA PROBAR
        try {
            return DriverManager.getConnection("jdbc:postgresql://localhost:5432/tu_base_datos", "postgres", "tu_password");
        } catch (Exception e) {
            return null;
        }
    }
}