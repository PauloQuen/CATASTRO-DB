import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class SistemaCatastro extends JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();

    public SistemaCatastro() {
        super("Sistema de Gestión Catastral Municipal");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1050, 700); // Un poco más amplio para el panel maestro-detalle de reportes
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(buildContentWrapper(), BorderLayout.CENTER);

        setContentPane(root);
        selectButton("inicio");
    }

    // ---------- PANEL LATERAL REESTRUCTURADO ----------
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(Color.LIGHT_GRAY);
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15));

        JLabel logo = new JLabel("Plataforma Catastral");
        logo.setFont(new Font("SansSerif", Font.BOLD, 16));
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(logo);

        sidebar.add(Box.createVerticalStrut(25)); 

        addNavButton(sidebar, "inicio", "Inicio");
        sidebar.add(Box.createVerticalStrut(10));
        addNavButton(sidebar, "vistas", "Vistas Estándar");
        sidebar.add(Box.createVerticalStrut(10));
        addNavButton(sidebar, "informes_operaciones", "Informes y Procesos");
        sidebar.add(Box.createVerticalStrut(10));
        addNavButton(sidebar, "registros", "Gestión de Registros");

        sidebar.add(Box.createVerticalGlue());
        addNavButton(sidebar, "salir", "Salir del Sistema");

        return sidebar;
    }

    private void addNavButton(JPanel sidebar, String key, String text) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(210, 40)); 
        button.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        button.addActionListener(e -> {
            if ("salir".equals(key)) {
                int confirm = JOptionPane.showConfirmDialog(this, "¿Desea salir?", "Confirmar", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) System.exit(0);
                return;
            }
            selectButton(key);
            cardLayout.show(contentPanel, key);
        });

        navButtons.put(key, button);
        sidebar.add(button);
    }

    private void selectButton(String key) {
        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            entry.getValue().setBackground(entry.getKey().equals(key) ? Color.WHITE : null);
        }
    }

    private JPanel buildContentWrapper() {
        contentPanel.add(buildWelcomePanel(), "inicio");
        contentPanel.add(buildSelectorSistemasPanel("Vistas Estándar"), "vistas");
        contentPanel.add(buildInformesYProcesosPanel(), "informes_operaciones");
        contentPanel.add(buildSelectorRegistrosPanel(), "registros");

        return contentPanel;
    }

    private JPanel buildWelcomePanel() {
        JPanel panel = new JPanel(new GridBagLayout()); 
        JLabel title = new JLabel("¡Bienvenido al Sistema de Gestión Catastral!");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        panel.add(title);
        return panel;
    }

    // ==================================================================================
    // ---------- MÓDULO CENTRALIZADO: CONTENEDOR DE PROCEDIMIENTOS ALMACENADOS ------
    // ==================================================================================
    private JPanel buildInformesYProcesosPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JLabel title = new JLabel("Consola de Operaciones y Procesos Almacenados en Base de Datos", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
        mainPanel.add(title, BorderLayout.NORTH);

        // Instanciamos los 3 nuevos paneles lógicos organizados
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Apertura y Cierre Presupuestal", new PROCESOS.PresupuestoAnualPanel());
        tabbedPane.addTab("Emisiones y Transferencias", new PROCESOS.EmisionesYTransferenciasPanel());
        tabbedPane.addTab("Auditoría Maestro-Detalle", new PROCESOS.ReportesAuditoriaPanel());

        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        return mainPanel;
    }

    // ---------- SELECCIÓN DE REGISTROS CENTRALIZADA (CRUDS) ----------
    private JPanel buildSelectorRegistrosPanel() {
        CardLayout localLayout = new CardLayout();
        JPanel localContainer = new JPanel(localLayout);

        JPanel menuPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(15, 15, 15, 15);

        JLabel subTitulo = new JLabel("Módulos de Actualización y Registro");
        subTitulo.setFont(new Font("SansSerif", Font.BOLD, 18));
        menuPanel.add(subTitulo, gbc);

        // BLOQUE CORE
        JPanel pnlPrincipales = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        pnlPrincipales.setBorder(BorderFactory.createTitledBorder(" Operaciones Core / Principales "));
        JButton btnPredios = new JButton("Gestión de Predios");
        JButton btnContribuyentes = new JButton("Control de Contribuyentes");
        JButton btnTributos = new JButton("Impuestos y Tributos");
        pnlPrincipales.add(btnPredios);
        pnlPrincipales.add(btnContribuyentes);
        pnlPrincipales.add(btnTributos);

        gbc.gridy = 1;
        menuPanel.add(pnlPrincipales, gbc);

        // BLOQUE CATÁLOGOS SECUNDARIOS
        JPanel pnlSoporte = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        pnlSoporte.setBorder(BorderFactory.createTitledBorder(" Tablas de Soporte Técnico "));
        JButton btnSoporteTablas = new JButton("Configurar Catálogos de Referencia");
        pnlSoporte.add(btnSoporteTablas);

        gbc.gridy = 2;
        menuPanel.add(pnlSoporte, gbc);

        localContainer.add(menuPanel, "menu_seleccion");

        localContainer.add(buildSubMenuPrediosPanel(localContainer), "menu_predios");
        localContainer.add(buildSubMenuContribuyentesPanel(localContainer), "menu_contribuyentes");
        localContainer.add(buildSubMenuReferencialesPanel(localContainer), "menu_referenciales");
        localContainer.add(buildFormPanel(localContainer, "Liquidación de Tributos", "menu_seleccion"), "pantalla_tributos");

        btnPredios.addActionListener(e -> localLayout.show(localContainer, "menu_predios"));
        btnContribuyentes.addActionListener(e -> localLayout.show(localContainer, "menu_contribuyentes"));
        btnTributos.addActionListener(e -> localLayout.show(localContainer, "pantalla_tributos"));
        btnSoporteTablas.addActionListener(e -> localLayout.show(localContainer, "menu_referenciales"));

        return localContainer;
    }

    // ---------- SUBMENÚ: 7 ENTIDADES DE PREDIOS ----------
    private JPanel buildSubMenuPrediosPanel(JPanel contenedorPadre) {
        CardLayout subLayout = new CardLayout();
        JPanel subContainer = new JPanel(subLayout);

        JPanel menuBotonesPanel = new JPanel(new BorderLayout());
        
        JButton btnVolverAtras = new JButton("← Volver al Panel de Selección");
        btnVolverAtras.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnVolverAtras.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnVolverAtras.addActionListener(e -> {
            CardLayout layout = (CardLayout) contenedorPadre.getLayout();
            layout.show(contenedorPadre, "menu_seleccion");
        });
        
        JPanel pnlSupWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlSupWrapper.add(btnVolverAtras);
        menuBotonesPanel.add(pnlSupWrapper, BorderLayout.NORTH);

        JPanel centroPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(10, 15, 10, 15);

        JLabel titulo = new JLabel("Módulo de Predios Urbanos y Estructuras");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 18));
        centroPanel.add(titulo, gbc);

        JPanel pnlFisico = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        pnlFisico.setBorder(BorderFactory.createTitledBorder(" Ubicación y Unidades Catastrales "));
        JButton btnDireccion = new JButton("Direcciones");
        JButton btnVivienda = new JButton("Viviendas Básicas");
        JButton btnPartida = new JButton("Partidas Registrales");
        JButton btnValor = new JButton("Valores Catastrales");
        pnlFisico.add(btnDireccion); pnlFisico.add(btnVivienda); pnlFisico.add(btnPartida); pnlFisico.add(btnValor);

        gbc.gridy = 1;
        centroPanel.add(pnlFisico, gbc);

        JPanel pnlSubtipos = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        pnlSubtipos.setBorder(BorderFactory.createTitledBorder(" Tipificaciones de Estructuras "));
        JButton btnCasaPart = new JButton("Casas Particulares");
        JButton btnBloque = new JButton("Bloque de Casas");
        JButton btnDep = new JButton("Departamentos");
        pnlSubtipos.add(btnCasaPart); pnlSubtipos.add(btnBloque); pnlSubtipos.add(btnDep);

        gbc.gridy = 2;
        centroPanel.add(pnlSubtipos, gbc);
        menuBotonesPanel.add(centroPanel, BorderLayout.CENTER);

        subContainer.add(menuBotonesPanel, "seleccion_crud_predios");

        subContainer.add(new CRUDS.PREDIOS.DireccionSwing(subContainer, "seleccion_crud_predios"), "crud_direccion");
        subContainer.add(new CRUDS.PREDIOS.ViviendaSwing(subContainer, "seleccion_crud_predios"), "crud_vivienda");
        subContainer.add(new CRUDS.PREDIOS.PartidaRegistralSwing(subContainer, "seleccion_crud_predios"), "crud_partida");
        subContainer.add(new CRUDS.PREDIOS.ValorCatastralSwing(subContainer, "seleccion_crud_predios"), "crud_valor");
        subContainer.add(new CRUDS.PREDIOS.CasaParticularSwing(subContainer, "seleccion_crud_predios"), "crud_casa_particular");
        subContainer.add(new CRUDS.PREDIOS.BloqueCasasSwing(subContainer, "seleccion_crud_predios"), "crud_bloque_casas");
        subContainer.add(new CRUDS.PREDIOS.DepartamentoSwing(subContainer, "seleccion_crud_predios"), "crud_departamento");

        btnDireccion.addActionListener(e -> subLayout.show(subContainer, "crud_direccion"));
        btnVivienda.addActionListener(e -> subLayout.show(subContainer, "crud_vivienda"));
        btnPartida.addActionListener(e -> subLayout.show(subContainer, "crud_partida"));
        btnValor.addActionListener(e -> subLayout.show(subContainer, "crud_valor"));
        btnCasaPart.addActionListener(e -> subLayout.show(subContainer, "crud_casa_particular"));
        btnBloque.addActionListener(e -> subLayout.show(subContainer, "crud_bloque_casas"));
        btnDep.addActionListener(e -> subLayout.show(subContainer, "crud_departamento"));

        return subContainer;
    }

    // ---------- SUBMENÚ: 5 ENTIDADES DE CONTRIBUYENTES ----------
    private JPanel buildSubMenuContribuyentesPanel(JPanel contenedorPadre) {
        CardLayout subLayout = new CardLayout();
        JPanel subContainer = new JPanel(subLayout);

        JPanel menuBotonesPanel = new JPanel(new BorderLayout());
        
        JButton btnVolverAtras = new JButton("← Volver al Panel de Selección");
        btnVolverAtras.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnVolverAtras.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnVolverAtras.addActionListener(e -> {
            CardLayout layout = (CardLayout) contenedorPadre.getLayout();
            layout.show(contenedorPadre, "menu_seleccion");
        });
        
        JPanel pnlSupWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlSupWrapper.add(btnVolverAtras);
        menuBotonesPanel.add(pnlSupWrapper, BorderLayout.NORTH);

        JPanel centroPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(10, 15, 10, 15);

        JLabel titulo = new JLabel("Registro Central de Contribuyentes e Historial Familiar");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 18));
        centroPanel.add(titulo, gbc);

        JPanel pnlMaestras = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        pnlMaestras.setBorder(BorderFactory.createTitledBorder(" Mantenimiento de Entidades Base "));
        JButton btnPersona = new JButton("Personas (h6m_persona)");
        JButton btnFamilia = new JButton("Familias (h7m_familia)");
        JButton btnPropietario = new JButton("Propietarios (h8m_propietario)");
        pnlMaestras.add(btnPersona); pnlMaestras.add(btnFamilia); pnlMaestras.add(btnPropietario);

        gbc.gridy = 1;
        centroPanel.add(pnlMaestras, gbc);

        JPanel pnlRelacionales = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        pnlRelacionales.setBorder(BorderFactory.createTitledBorder(" Estructura Familiar y Vivienda "));
        JButton btnCabFamilia = new JButton("Jefes de Familia (h6m_cab_familia)");
        JButton btnPropVivienda = new JButton("Asignación de Viviendas (h8m_prop_vivienda)");
        pnlRelacionales.add(btnCabFamilia); pnlRelacionales.add(btnPropVivienda);

        gbc.gridy = 2;
        centroPanel.add(pnlRelacionales, gbc);
        menuBotonesPanel.add(centroPanel, BorderLayout.CENTER);

        subContainer.add(menuBotonesPanel, "seleccion_crud_contribuyentes");

        subContainer.add(new CRUDS.CONTRIBUYENTES.PersonaSwing(subContainer, "seleccion_crud_contribuyentes"), "crud_persona");
        subContainer.add(new CRUDS.CONTRIBUYENTES.PropietarioSwing(subContainer, "seleccion_crud_contribuyentes"), "crud_propietario");
        subContainer.add(new CRUDS.CONTRIBUYENTES.FamiliaSwing(subContainer, "seleccion_crud_contribuyentes"), "crud_familia");
        subContainer.add(new CRUDS.CONTRIBUYENTES.CabFamiliaSwing(subContainer, "seleccion_crud_contribuyentes"), "crud_cab_familia");
        subContainer.add(new CRUDS.CONTRIBUYENTES.PropViviendaSwing(subContainer, "seleccion_crud_contribuyentes"), "crud_prop_vivienda");

        btnPersona.addActionListener(e -> subLayout.show(subContainer, "crud_persona"));
        btnFamilia.addActionListener(e -> subLayout.show(subContainer, "crud_familia"));
        btnPropietario.addActionListener(e -> subLayout.show(subContainer, "crud_propietario"));
        btnCabFamilia.addActionListener(e -> subLayout.show(subContainer, "crud_cab_familia"));
        btnPropVivienda.addActionListener(e -> subLayout.show(subContainer, "crud_prop_vivienda"));

        return subContainer;
    }

    // ---------- SUBMENÚ: 9 ENTIDADES REFERENCIALES / SOPORTE ----------
    private JPanel buildSubMenuReferencialesPanel(JPanel contenedorPadre) {
        CardLayout subLayout = new CardLayout();
        JPanel subContainer = new JPanel(subLayout);

        JPanel menuBotonesPanel = new JPanel(new BorderLayout());
        
        JButton btnVolverAtras = new JButton("← Volver al Panel de Selección");
        btnVolverAtras.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnVolverAtras.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnVolverAtras.addActionListener(e -> {
            CardLayout layout = (CardLayout) contenedorPadre.getLayout();
            layout.show(contenedorPadre, "menu_seleccion");
        });
        
        JPanel pnlSupWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlSupWrapper.add(btnVolverAtras);
        menuBotonesPanel.add(pnlSupWrapper, BorderLayout.NORTH);

        JPanel centroPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(8, 12, 8, 12);

        JLabel titulo = new JLabel("Tablas Referenciales y Mantenimiento de Catálogos");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 18));
        centroPanel.add(titulo, gbc);

        JPanel pnlGeo = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        pnlGeo.setBorder(BorderFactory.createTitledBorder(" Estructura Político-Geográfica "));
        JButton btnRegion = new JButton("Región");
        JButton btnProvincia = new JButton("Provincia");
        JButton btnDistrito = new JButton("Distrito");
        JButton btnMuni = new JButton("Municipalidad");
        pnlGeo.add(btnRegion); pnlGeo.add(btnProvincia); pnlGeo.add(btnDistrito); pnlGeo.add(btnMuni);

        gbc.gridy = 1;
        centroPanel.add(pnlGeo, gbc);

        JPanel pnlUrbano = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        pnlUrbano.setBorder(BorderFactory.createTitledBorder(" Catálogos de Tipificación Urbana "));
        JButton btnZona = new JButton("Zonas");
        JButton btnViaTipo = new JButton("Tipos de Vías");
        JButton btnTipoPredio = new JButton("Tipos de Predio");
        JButton btnUsoPredio = new JButton("Usos de Predio");
        JButton btnEscala = new JButton("Escala Tributos");
        pnlUrbano.add(btnZona); pnlUrbano.add(btnViaTipo); pnlUrbano.add(btnTipoPredio); pnlUrbano.add(btnUsoPredio); pnlUrbano.add(btnEscala);

        gbc.gridy = 2;
        centroPanel.add(pnlUrbano, gbc);
        menuBotonesPanel.add(centroPanel, BorderLayout.CENTER);

        subContainer.add(menuBotonesPanel, "seleccion_crud_referenciales");

        subContainer.add(new CRUDS.REFERENCIALES.RegionSwing(subContainer, "seleccion_crud_referenciales"), "crud_region");
        subContainer.add(new CRUDS.REFERENCIALES.ProvinciaSwing(subContainer, "seleccion_crud_referenciales"), "crud_provincia");
        subContainer.add(new CRUDS.REFERENCIALES.DistritoSwing(subContainer, "seleccion_crud_referenciales"), "crud_distrito");
        subContainer.add(new CRUDS.REFERENCIALES.MunicipalidadSwing(subContainer, "seleccion_crud_referenciales"), "crud_municipalidad");
        subContainer.add(new CRUDS.REFERENCIALES.ZonaSwing(subContainer, "seleccion_crud_referenciales"), "crud_zona");
        subContainer.add(new CRUDS.REFERENCIALES.TipoViaSwing(subContainer, "seleccion_crud_referenciales"), "crud_via_tipo");
        subContainer.add(new CRUDS.REFERENCIALES.TipoPredioSwing(subContainer, "seleccion_crud_referenciales"), "crud_tipo_predio");
        subContainer.add(new CRUDS.REFERENCIALES.UsoPredioSwing(subContainer, "seleccion_crud_referenciales"), "crud_uso_predio");
        subContainer.add(new CRUDS.REFERENCIALES.EscalaTributoSwing(subContainer, "seleccion_crud_referenciales"), "crud_escala");

        btnRegion.addActionListener(e -> subLayout.show(subContainer, "crud_region"));
        btnProvincia.addActionListener(e -> subLayout.show(subContainer, "crud_provincia"));
        btnDistrito.addActionListener(e -> subLayout.show(subContainer, "crud_distrito"));
        btnMuni.addActionListener(e -> subLayout.show(subContainer, "crud_municipalidad"));
        btnZona.addActionListener(e -> subLayout.show(subContainer, "crud_zona"));
        btnViaTipo.addActionListener(e -> subLayout.show(subContainer, "crud_via_tipo"));
        btnTipoPredio.addActionListener(e -> subLayout.show(subContainer, "crud_tipo_predio"));
        btnUsoPredio.addActionListener(e -> subLayout.show(subContainer, "crud_uso_predio"));
        btnEscala.addActionListener(e -> subLayout.show(subContainer, "crud_escala"));

        return subContainer;
    }

    // ---------- MÓDULO CONSULTOR CENTRALIZADO (VISTAS ESTÁNDAR) ----------
    private JPanel buildSelectorSistemasPanel(String tipoPanel) {
        CardLayout localLayout = new CardLayout();
        JPanel localContainer = new JPanel(localLayout);

        JPanel menuPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(15, 15, 15, 15);

        JLabel subTitulo = new JLabel("Módulo Consultor: " + tipoPanel);
        subTitulo.setFont(new Font("SansSerif", Font.BOLD, 18));
        menuPanel.add(subTitulo, gbc);

        JPanel panelBotonesRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 15));
        panelBotonesRow.setBorder(BorderFactory.createTitledBorder(" Seleccione un Área de Consulta de Datos "));
        JButton btnC = new JButton("Sistema Catastral (C)");
        JButton btnH = new JButton("Sistema Habitacional (H)");
        JButton btnP = new JButton("Sistema Presupuestario (P)");
        
        panelBotonesRow.add(btnC); panelBotonesRow.add(btnH); panelBotonesRow.add(btnP);

        gbc.gridy = 1;
        menuPanel.add(panelBotonesRow, gbc);
        localContainer.add(menuPanel, "menu_seleccion");

        // SUBMENÚS INTERMEDIOS (Sistemas C, H y P)
        JPanel pnlCatastral = new JPanel(new BorderLayout());
        pnlCatastral.add(buildSubMenuHeader(localContainer, "menu_seleccion"), BorderLayout.NORTH);
        JPanel pnlBotonesC = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        pnlBotonesC.setBorder(BorderFactory.createTitledBorder(" Vistas del Territorio e Infraestructura "));
        JButton btnC1 = new JButton("Ficha de Predios Detallados");
        JButton btnC2 = new JButton("Viviendas Completas e Infraestructura");
        pnlBotonesC.add(btnC1); pnlBotonesC.add(btnC2);
        pnlCatastral.add(pnlBotonesC, BorderLayout.CENTER);
        localContainer.add(pnlCatastral, "submenu_C");

        JPanel pnlHabitacional = new JPanel(new BorderLayout());
        pnlHabitacional.add(buildSubMenuHeader(localContainer, "menu_seleccion"), BorderLayout.NORTH);
        JPanel pnlBotonesH = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        pnlBotonesH.setBorder(BorderFactory.createTitledBorder(" Padrón y Núcleos Familiares "));
        JButton btnH1 = new JButton("Padrón de Propietarios y Personas");
        JButton btnH2 = new JButton("Historial de Propietarios por Predio");
        pnlBotonesH.add(btnH1); pnlBotonesH.add(btnH2);
        pnlHabitacional.add(pnlBotonesH, BorderLayout.CENTER);
        localContainer.add(pnlHabitacional, "submenu_H");

        JPanel pnlPresupuestario = new JPanel(new BorderLayout());
        pnlPresupuestario.add(buildSubMenuHeader(localContainer, "menu_seleccion"), BorderLayout.NORTH);
        JPanel pnlBotonesP = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        pnlBotonesP.setBorder(BorderFactory.createTitledBorder(" Estados Financieros y Tributos Colectados "));
        JButton btnP1 = new JButton("Estado de Deudas Tributarias");
        JButton btnP2 = new JButton("Balance Recaudación vs Presupuesto");
        pnlBotonesP.add(btnP1); pnlBotonesP.add(btnP2);
        pnlPresupuestario.add(pnlBotonesP, BorderLayout.CENTER);
        localContainer.add(pnlPresupuestario, "submenu_P");

        btnC.addActionListener(e -> localLayout.show(localContainer, "submenu_C"));
        btnH.addActionListener(e -> localLayout.show(localContainer, "submenu_H"));
        btnP.addActionListener(e -> localLayout.show(localContainer, "submenu_P"));

        // Inyecciones Físicas de las 6 Vistas Estándar
        localContainer.add(new VIEWS.SISTEMA_CATASTRAL.VwPredioDetalladoSwing(localContainer, "submenu_C"), "data_c1");
        localContainer.add(new VIEWS.SISTEMA_CATASTRAL.VwViviendaCompletaSwing(localContainer, "submenu_C"), "data_c2");
        localContainer.add(new VIEWS.SISTEMA_HABITACIONAL.VwPropietarioPersonaSwing(localContainer, "submenu_H"), "data_h1");
        localContainer.add(new VIEWS.SISTEMA_HABITACIONAL.VwHistorialPropietariosPredioSwing(localContainer, "submenu_H"), "data_h2");
        localContainer.add(new VIEWS.SISTEMA_TRIBUTARIO.VwDeudaTributariaSwing(localContainer, "submenu_P"), "data_p1");
        localContainer.add(new VIEWS.SISTEMA_TRIBUTARIO.VwRecaudacionVsPresupuestoSwing(localContainer, "submenu_P"), "data_p2");
        
        btnC1.addActionListener(e -> localLayout.show(localContainer, "data_c1"));
        btnC2.addActionListener(e -> localLayout.show(localContainer, "data_c2"));
        btnH1.addActionListener(e -> localLayout.show(localContainer, "data_h1"));
        btnH2.addActionListener(e -> localLayout.show(localContainer, "data_h2"));
        btnP1.addActionListener(e -> localLayout.show(localContainer, "data_p1"));
        btnP2.addActionListener(e -> localLayout.show(localContainer, "data_p2"));

        return localContainer;
    }
    
    private JPanel buildSubMenuHeader(JPanel contenedorPadre, String destinoRetorno) {
        JPanel pnlHeader = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnVolverAtras = new JButton("← Volver a Selección de Área");
        btnVolverAtras.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnVolverAtras.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnVolverAtras.addActionListener(e -> {
            CardLayout layout = (CardLayout) contenedorPadre.getLayout();
            layout.show(contenedorPadre, destinoRetorno);
        });
        pnlHeader.add(btnVolverAtras);
        return pnlHeader;
    }

    private JPanel buildFormPanel(JPanel contenedorPadre, String name, String destinoRetorno) {
        JPanel panel = new JPanel(new BorderLayout());
        
        JButton btnVolver = new JButton("← Volver al Panel de Selección");
        btnVolver.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnVolver.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnVolver.addActionListener(e -> {
            CardLayout layout = (CardLayout) contenedorPadre.getLayout();
            layout.show(contenedorPadre, destinoRetorno);
        });
        
        JPanel pnlSuperiorWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlSuperiorWrapper.add(btnVolver);
        panel.add(pnlSuperiorWrapper, BorderLayout.NORTH);

        JLabel label = new JLabel("<html><center>" + name + "<br><span style='font-size:11px;color:gray;'>Entorno de Datos Conectado</span></center></html>", SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.PLAIN, 18));
        panel.add(label, BorderLayout.CENTER);
        
        return panel;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new SistemaCatastro().setVisible(true);
        });
    }
}
