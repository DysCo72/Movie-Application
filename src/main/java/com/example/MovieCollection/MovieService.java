package com.example.MovieCollection;

import org.springframework.beans.factory.annotation.Autowired;
import static org.apache.commons.lang3.StringUtils.isBlank;
import org.springframework.stereotype.Service;

import com.example.Repository.MovieRepo;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.opencsv.CSVReader;

import info.movito.themoviedbapi.TmdbApi;
import info.movito.themoviedbapi.TmdbSearch;
import info.movito.themoviedbapi.model.MovieDb;
import info.movito.themoviedbapi.model.core.MovieResultsPage;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.persistence.Column;

@Service
public class MovieService {
		@Autowired
		//initialized as null for some reason until I add PostConstruct on voidinitMovieCollection & leave constructor for MovieService empty
        private MovieRepo repo; 
		//@Autowired
		//private List<Movie> movies = new ArrayList<Movie>();
		//@Autowired
		private TmdbSearch searchApi = new TmdbApi("01b13b38fd2da4ca845144d2cedd9762").getSearch();
		
        public MovieService() {
        }
        @PostConstruct
        public void initMovieCollection() {
        	String path = "the_oscar_award.csv";
    		String[] line;
    		try {
    			CSVReader csvReader = new CSVReader(new FileReader(path));
    			//reads line to skip category portion of file
    			line = csvReader.readNext();
    			if(line!=null) {
    				int id = 1;
        			//boolean skip = false;
    				while((line = csvReader.readNext()) != null) {
    					//0:yearReleased, 1:yearNominated, 3:category, 4:awardee, 5:title, 6:awardStatus
    					if(line[5] == null)
    						line[5] = "N/A";
    					//id, title, category, yearReleased, yearNominated, awardee, status
    					Movie mov = new Movie(id,line[5],line[3],Integer.parseInt(line[0]),Integer.parseInt(line[1]),line[4],Boolean.parseBoolean(line[6]));
    					//movies.add(mov);
    					repo.save(mov);
    					id++;
    				}
    			}
    		} catch(FileNotFoundException e) {
    			e.printStackTrace();
    		} catch(IOException e) {
    			e.printStackTrace();
    		}
        }
        public String getImageLink(String poster) {
        	return "https://image.tmdb.org/t/p/w500" + poster;
        }
        public String getWatchLink(int tmdbId) {
        	return "https://www.themoviedb.org/movie/" + tmdbId+"/watch";
        }
        
    public List<Movie> updateSearchedResults(List<Movie> list) {
    	for(int i = 0; i < list.size(); i++) {
    		Movie mov = list.get(i);
    		if(mov.getPlot() == null) {
    			MovieResultsPage results;
    			if(!isBlank(mov.getTitle())) {
    				int year = mov.getYearReleased();
    				while((results = searchApi.searchMovie(mov.getTitle(), year, null, true, 0)).getTotalResults() == 0)
    					year++;
    				for(MovieDb m : results) {
    					int apiId = m.getId();
    					mov.setTmdbId(apiId);
    					mov.setImageLink(getImageLink(m.getPosterPath()));
    					mov.setLink(getWatchLink(apiId));
    					mov.setPlot(m.getOverview());
    					break;
    				}
    				repo.save(mov);
    			}
    		}
    	}
    	return list;
    }
    //DATABASE METHODS
    public Movie findById(int id) {
    	List<Movie> temp = new ArrayList<Movie>();
    	temp.add(repo.findById(id));
    	return updateSearchedResults(temp).get(0);
    }
    public void deleteMovieByIdFromDatabase(int id) {
    	repo.deleteById(id);
    }
    public void addMovieToDatabase(Movie mov) {
    	repo.save(mov);
    }
    public List<Movie> findByYear(int year) {
    	return updateSearchedResults(repo.findByYear(year));
    }
    public List<Movie> findWinnersByCategory(String award, int year) {
    	return updateSearchedResults(repo.findWinnerByYear(award, year));
    }
    public List<Movie> findNominationsByCategoryAndYear(String award, int year) {
    	return updateSearchedResults(repo.findNominationsByYear(award, year));
    }
    //possible method to use for custom searches
    public List<Movie> findByTitle(String title) {
    	String searchTitle = title.replaceAll("_", " ");
    	//System.out.println(searchTitle);
    	return updateSearchedResults(repo.findByTitleContainingIgnoreCase(searchTitle));
    }
    //haven't tested this yet
    public void updateMovieInDatabase(int id, Movie mov) {
    	Movie temp = repo.findById(id);
    	temp.setAwardee(mov.getAwardee());
    	temp.setAwardStatus(mov.isAwardStatus());
    	temp.setCategory(mov.getCategory());
    	temp.setImageLink(mov.getImageLink());
    	temp.setLink(mov.getLink());
    	temp.setPlot(mov.getPlot());
    	temp.setTitle(mov.getTitle());
    	temp.setTmdbId(mov.getTmdbId());
    	temp.setYearNominated(mov.getYearNominated());
    	temp.setYearReleased(mov.getYearReleased());
    	repo.save(temp);
    }
}
