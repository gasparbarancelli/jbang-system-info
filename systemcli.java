///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.5.0

import com.sun.management.OperatingSystemMXBean;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Command(name = "systemcli", mixinStandardHelpOptions = true, version = "systemcli 1.0", description = "systemcli made with jbang")
class systemcli implements Callable<Integer> {

    @Option(names = { "-all"}, description = "Exibe todas informacoes")
    private boolean showAll;

    @Option(names = { "-d", "--disk" }, description = "Diretorio que deseja obter as informacoes de espaco em disco")
    private String disk = null;

    public static void main(String... args) {
        int exitCode = new CommandLine(new systemcli()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        if (showAll) {
            systemcli object = systemcli.class.newInstance();
            Arrays.stream(systemcli.class.getMethods())
                    .filter(method -> method.getName().startsWith("show"))
                    .sorted(Comparator.comparing(Method::getName))
                    .forEach(method -> {
                        try {
                            Object arg = method.getName().equals("showPortInUse") ? "8080" : true;
                            method.invoke(object, arg);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
        }
        return 0;
    }

    @Option(names = { "-uc", "--user-country" }, description = "Exibe o pais do usuario")
    public void showUserCountry(boolean show) {
        String userCountry = System.getProperties().getProperty("user.country");
        print("Pais do usuario", userCountry);
    }

    @Option(names = { "-uh", "--user-home" }, description = "Exibe a pasta do usuario")
    public void showUserHome(boolean show) {
        String userHome = System.getProperties().getProperty("user.home");
        print("Pasta home do usuario", userHome);
    }

    @Option(names = { "-ul", "--user-language" }, description = "Exibe a lingua do usuario")
    public void showUserLanguage(boolean show) {
        String userLanguage = System.getProperties().getProperty("user.language");
        print("Lingua do usuario", userLanguage);
    }

    @Option(names = { "-un", "--user-name" }, description = "Exibe o nome do usuario")
    public void showUserName(boolean show) {
        String userName = System.getProperties().getProperty("user.name");
        print("Nome do usuario", userName);
    }

    @Option(names = { "-ut", "--user-timezone" }, description = "Exibe o fuso horario do usuario")
    public void showUserTimezone(boolean show) {
        String userTimezone = System.getProperties().getProperty("user.timezone");
        print("Fuso horario do usuario", userTimezone);
    }

    @Option(names = { "-sa", "--system-arch" }, description = "Exibe a arquitetura do sistema operacional")
    public void showOsArch(boolean show) {
        String osArch = System.getProperties().getProperty("os.arch");
        print("Arquitetura do sistema operacional", osArch);
    }

    @Option(names = { "-sn", "--system-name" }, description = "Exibe o nome do sistema operacional")
    public void showOsName(boolean show) {
        String osName = System.getProperties().getProperty("os.name");
        print("Nome do sistema operacional", osName);
    }

    @Option(names = { "-sv", "--system-version" }, description = "Exibe a verssao do sistema operacional")
    public void showOsVersion(boolean show) {
        String osVersion = System.getProperties().getProperty("os.version");
        print("Versao do sistema operacional", osVersion);
    }

    @Option(names = { "-c", "--cpu" }, description = "Exibe o uso de CPU")
    public void showCpu(boolean show) {
        OperatingSystemMXBean mbean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        double load = mbean.getSystemCpuLoad();
        for(int i=0; i<10; i++) {
            load = mbean.getSystemCpuLoad();
            if((load<0.0 || load>1.0) && load != -1.0) {
                throw new RuntimeException("Nao foi possivel obter um valor valido de consumo do cpu");
            }
            try {
                Thread.sleep(200);
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
        }
        String usoDoCpu = String.format("%.2f", (load * 100));
        print("Uso do CPU",  usoDoCpu + "%");
    }

    @Option(names = { "-tm", "--total-memory" }, description = "Exibe o total de memoria")
    public void showTotalMemory(boolean show) {
        OperatingSystemMXBean mxbean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        Long memory = mxbean.getTotalPhysicalMemorySize() / 1024 / 1024 / 1000;
        print("Total de memoria", longToStr(memory) + "GB");
    }

    @Option(names = { "-am", "--available-memory" }, description = "Exibe o total de memoria disponivel")
    public void showAvailableMemory(boolean show) {
        OperatingSystemMXBean mxbean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        Long memory = mxbean.getFreePhysicalMemorySize() / 1024 / 1024 / 1000;
        print("Memoria disponivel", longToStr(memory) + "GB");
    }

    @Option(names = { "-fd", "--free-disk" }, description = "Exibe o espaco em disco livre")
    public void showFreeDisk(boolean show) {
        Long total = 0L;

        if (disk != null) {
            total += new File(disk).getFreeSpace();
        } else {
            File[] f = File.listRoots();
            for (File file : f) {
                total += file.getFreeSpace();
            }
        }

        Long discoEmGb = total / 1024 / 1024 / 1000;
        print("Espaco em disco disponivel",  longToStr(discoEmGb) + "GB");
    }

    @Option(names = { "-td", "--total-disk" }, description = "Exibe o espaco total de disco")
    public void showTotalDisk(boolean show) {
        Long total = 0L;

        if (disk != null) {
            total += new File(disk).getTotalSpace();
        } else {
            File[] f = File.listRoots();
            for (File file : f) {
                total += file.getTotalSpace();
            }
        }

        Long discoEmGb = total / 1024 / 1024 / 1000;
        print("Espaco total de disco", longToStr(discoEmGb) + "GB");
    }

    @Option(names = { "-u", "--uptime" }, description = "Exibe há quanto tempo seu sistema está em atividade")
    public void showUptime(boolean show) throws Exception {
        long uptime = -1;
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            Process uptimeProc = Runtime.getRuntime().exec("wmic os get lastbootuptime");
            BufferedReader in = new BufferedReader(new InputStreamReader(uptimeProc.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("LastBootUpTime") || line.trim().equals("")) {
                    continue;
                }
                String lastBootUpTime = line.split("\\.")[0];
                SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
                Date bootTime = format.parse(lastBootUpTime);
                uptime = System.currentTimeMillis() - bootTime.getTime();
                break;
            }
        } else if (os.contains("mac") || os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            Process uptimeProc = Runtime.getRuntime().exec("uptime");
            BufferedReader in = new BufferedReader(new InputStreamReader(uptimeProc.getInputStream()));
            String line = in.readLine();
            if (line != null) {
                Pattern parse = Pattern.compile("(\\d+):(\\d+):(\\d+) up (\\d+)");
                Matcher matcher = parse.matcher(line);
                if (matcher.find()) {
                    String _days = matcher.group(4);
                    String _hours = matcher.group(1);
                    String _minutes = matcher.group(2);
                    int days = _days != null ? Integer.parseInt(_days) : 0;
                    int hours = _hours != null ? Integer.parseInt(_hours) : 0;
                    int minutes = _minutes != null ? Integer.parseInt(_minutes) : 0;
                    uptime = TimeUnit.MILLISECONDS.convert(days, TimeUnit.DAYS) +
                            TimeUnit.MILLISECONDS.convert(hours, TimeUnit.HOURS) +
                            TimeUnit.MILLISECONDS.convert(minutes, TimeUnit.MINUTES);
                }
            }
        }

        print("Tempo de atividade (horas) ", "" + TimeUnit.HOURS.convert(uptime, TimeUnit.MILLISECONDS));
        print("Tempo de atividade (dias) ", "" + TimeUnit.DAYS.convert(uptime, TimeUnit.MILLISECONDS));
    }

    @Option(names = { "-p", "--port" }, description = "Verifica se a porta esta em uso")
    public void showPortInUse(String port) throws Exception {
        if (port == null) {
            port = "8080";
        }
        String os = System.getProperty("os.name").toLowerCase();
        String[] command;
        if (os.startsWith("win")) {
            command = new String[]{"cmd", "/c", "netstat -a -n -o | findstr " + port};
        } else {
            command = new String[]{"/bin/bash", "-c", "netstat -tulpn | grep :" + port};
        }
        Process uptimeProc =  Runtime.getRuntime().exec(command);
        BufferedReader in = new BufferedReader(new InputStreamReader(uptimeProc.getInputStream(), StandardCharsets.UTF_8));
        String emUso = in.readLine() == null ? "Nao" : "Sim";
        print("Porta " + port + " esta em uso?", emUso);
    }

    private String longToStr(Long value) {
        DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(Locale.US);
        DecimalFormatSymbols symbols = formatter.getDecimalFormatSymbols();

        symbols.setGroupingSeparator('.');
        formatter.setDecimalFormatSymbols(symbols);
        return formatter.format(value);
    }

    private String getLabel(String value) {
        StringBuilder newValue = new StringBuilder(value).append(" ");
        for (int i = value.length(); i < 40; i++) {
             newValue.append("-");
        }
        return newValue.append(" ").toString();
    }

    private void print(String label, String value) {
        System.out.println(getLabel(label.trim().toUpperCase()) + value);
    }
}
