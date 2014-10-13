
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * A class concerned with reloading a server-side cache of data, to avoid unnecessary 
 * and expensive calls outside the system (i.e. to persistence, to login server, etc).
 * 
 * @author Abby B. Bullock
 * 
 */
public class ProjectDataReloader {
    
    private final List<DataReloader> dataReloaders;
    
    /**
     * A fancy Java interface that handles the scheduling of tasks that need to
     * happen periodically.
     */
    private ScheduledExecutorService executor;
    
    /*
     * How often should the executor service reload our data?
     */
    private static final int RELOAD_PERIOD = 15;
    private static final TimeUnit RELOAD_PERIOD_UNIT = TimeUnit.SECONDS;
    
    private final Project project;
    
    /*
     * Factory method to create a ProjectDataReloader of the appropriate "type"
     * for a Project.
     */
    public static ProjectDataReloader getReloaderForType(Project project) {
        
        ProjectType type = project.getType();
        List<DataReloader> reloaders = new ArrayList<>();
        if (type.equals(ProjectType.STATIC)) {
            reloaders.add(new LoginStatisticsLoader(project));
            return new ProjectDataReloader(project, reloaders);
        } else if (type.equals(ProjectType.LIVE)) {
            reloaders.add(new ProjectDetailsLoader(project));
            reloaders.add(new LastUpdateTimeLoader(project));
            reloaders.add(new LoginStatisticsLoader(project));
            return new ProjectDataReloader(project, reloaders);
        }
        return null;
    }
    
    private ProjectDataReloader(Project project, List<DataReloader> dataReloaders) {
        this.project = project;
        this.dataReloaders = dataReloaders;
    }
    
    /**
     * Method to schedule the reloading of project data
     */
    private void scheduleDataLoading() {
        for (DataReloader reloader : dataReloaders ) { 
            executor.scheduleAtFixedRate(reloader, 0, RELOAD_PERIOD, RELOAD_PERIOD_UNIT);
        }
    }
    
    public void start() {
        
        System.out.println("Starting project data reloading thread for project \"" + project.getName() + "\", type: "
            + project.getType());
        executor = Executors.newScheduledThreadPool(4);
        scheduleDataLoading();
        executor.scheduleAtFixedRate(new Runnable() {
            
            @Override
            public void run() {
                //periodically print our project
                project.prettyPrint();
            }
        }, 1, RELOAD_PERIOD, RELOAD_PERIOD_UNIT);
    }
    
    public void stop() {
        
        System.out.println("Stopping project persistence reloading thread for project \"" + project.getName() + "\"...");
        
        executor.shutdown();
        
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            // we're shutting down anyway
        }
        
        executor.shutdownNow();
    }
    
    public static void main(String[] args) {
        ProjectDataReloader reloader1 = getReloaderForType(new Project("project1", ProjectType.STATIC));
        
        ProjectDataReloader reloader2 = getReloaderForType(new Project("project2", ProjectType.LIVE));
        
        reloader1.start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            
        }
        reloader2.start();
        
        try {
            Thread.sleep(180000);
        } catch (InterruptedException e) {
        }
        
        reloader1.stop(); reloader2.stop();
    }

}
