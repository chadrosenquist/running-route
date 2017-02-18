package com.kromracing.runningroute.selenium;


import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;

/**
 * Tests the Running Route website from the browser level.
 * 
 * Created August 2011
 *
 */
public class RunningRouteSelTest {
    private static final String RUNNING_ROUTE_URL = "http://127.0.0.1:8888/";
    
    private WebDriver driver = null;
    
    private static enum Browser {
        IE,
        FF,
    }

    /**
     * Create the WebDriver
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        final Browser browser = Browser.IE;
        switch (browser) {
        case FF:
            driver = new FirefoxDriver();
            break;
        case IE:
            driver = new InternetExplorerDriver();
            break;
        }
    }
    
    /**
     * Close the browser window.
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        if (driver != null) {
            driver.close();
            driver = null;
        }
    }
    
    /**
     * Verify the basic elements are on the webpage.
     * @throws Exception
     */
    @Test
    public void testBasicElements() throws Exception {                  
       driver.get(RUNNING_ROUTE_URL);
       
       // Verify location controls are on the screen.
       driver.findElement(By.id("label-location"));
       driver.findElement(By.id("textbox-location"));
       driver.findElement(By.id("button-search"));
       
       // Verify management controls are on the screen.
       driver.findElement(By.id("label-distance-literal"));
       driver.findElement(By.id("label-distance"));
       driver.findElement(By.id("button-undo"));
       driver.findElement(By.id("button-new-route"));
       driver.findElement(By.id("checkbox-follow-road"));
       
       //assertNotNull(searchButton);
       
  
       ///searchButton.sendKeys("60005");
       //searchButton.submit();
       //Thread.sleep(3000);
       //driver.close();
       //driver.

    }
    
    /**
     * Searches for various cities and verifies the center in the URL is correct.
     * @throws Exception
     */
    @Test
    public void testSearch() throws Exception {
        // Set zoom to 16 for this test case.
        driver.get(RUNNING_ROUTE_URL + "#z=16");
        
        final WebElement searchTextbox = driver.findElement(By.id("textbox-location"));
        final WebElement searchButton = driver.findElement(By.id("button-search"));
        
        // New York City
        searchTextbox.clear();
        searchTextbox.sendKeys("10001");
        searchButton.click();
        waitForGoogle();
        assertEquals("c=40.748328,-73.996225&z=16", getRouteInUrl());
        
        // Menlo Park, CA
        searchTextbox.clear();
        searchTextbox.sendKeys("menlo park, ca");
        searchButton.click();
        waitForGoogle();
        assertEquals("c=37.453827,-122.182187&z=16", getRouteInUrl());     
    }
    
    /**
     * Call while waiting for Google to come back.
     * Either after a search, or while waiting for follow the road.
     * Unfortunately, this website doesn't have any loading indicators
     * when making a Google call.  For now, just sleep.
     * @throws InterruptedException 
     */
    private void waitForGoogle() throws InterruptedException {
        Thread.sleep(3000);        
    }
    
    /**
     * Returns the route in the URL.
     * For example, if the URL is:
     *     http://127.0.0.1:8888/RunningRoute.html#c=40.750606,-73.993349&z=16
     * this function will return:
     *     c=40.750606,-73.993349&z=16
     * @return
     */
    private String getRouteInUrl() {
        final String url = driver.getCurrentUrl();
        final String[] urlParts = url.split("#");
        if (urlParts.length >= 2)
            return urlParts[1];
        else
            return null;
    }

}
