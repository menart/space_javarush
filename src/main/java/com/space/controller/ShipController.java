package com.space.controller;

import com.space.exceptions.NotFoundException;
import com.space.model.Ship;
import com.space.model.ShipType;
import com.space.repository.ShipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestController
public class ShipController {

    private final static Logger logger = LoggerFactory.getLogger(ShipController.class);

    private final ShipRepository shipRepository;

    public final Integer nowYear = 3019;



    @Autowired
    public ShipController(ShipRepository shipRepository) {
        this.shipRepository = shipRepository;
    }

    @PostMapping(value = "/rest/ships/{id}")
    public ResponseEntity<Ship> updateShip(@PathVariable Long id, @RequestBody Ship ship){
        boolean update = false;
        if(id == null || id <= 0)
            return new ResponseEntity(null,HttpStatus.BAD_REQUEST);
        Ship updateShip;
        try {
            updateShip = shipRepository.findById(id).get();
        }catch (NoSuchElementException exp){
            throw new NotFoundException();
        }

        if(ship == null)
            return new ResponseEntity(updateShip,HttpStatus.OK);

        if(ship.getName() !=null){
            if(ship.getName().trim().length()==0 || ship.getName().length()>50)
                return new ResponseEntity(null,HttpStatus.BAD_REQUEST);
            updateShip.setName(ship.getName());
            update = true;
        }

        if(ship.getPlanet() !=null){
            if(ship.getPlanet().trim().length() == 0 || ship.getPlanet().length()>50)
                return new ResponseEntity(null,HttpStatus.BAD_REQUEST);
            updateShip.setPlanet(ship.getPlanet());
            update = true;
        }

        if(ship.getShipType() != null) {
            updateShip.setShipType(ship.getShipType());
            update = true;
        }

        if(ship.getProdDate() != null){
            if(ship.getProdDate().getYear() + 1900 < 2800  || ship.getProdDate().getYear() + 1900 > 3019)
                return new ResponseEntity(null,HttpStatus.BAD_REQUEST);
            updateShip.setProdDate(ship.getProdDate());
            update = true;
        }

        if(ship.getUsed() != null) {
            updateShip.setUsed(ship.getUsed());
            update = true;
        }

        if(ship.getUsed() != null){
            if(((Long)Math.round(ship.getSpeed()*100)).doubleValue()/100.00  < 0.01 ||
                    ((Long)Math.round(ship.getSpeed()*100)).doubleValue()/100.00  > 0.99)
                return new ResponseEntity(null,HttpStatus.BAD_REQUEST);
            updateShip.setSpeed(((Long)Math.round(ship.getSpeed()*100)).doubleValue()/100.00);
            update = true;
        }

        if(ship.getCrewSize() != null){
            if(ship.getCrewSize() < 1 || ship.getCrewSize() > 9999)
                return new ResponseEntity(null,HttpStatus.BAD_REQUEST);
            updateShip.setCrewSize(ship.getCrewSize());
            update = true;
        }

        if(update) {
            updateShip.updateRating(nowYear);
            shipRepository.save(updateShip);
        }

        return new ResponseEntity(updateShip,HttpStatus.OK);
    }

    @DeleteMapping(value = "/rest/ships/{id}")
    public ResponseEntity<Ship> deleteShip(@PathVariable Long id){
        if(id == null || id <= 0)
            return new ResponseEntity(null,HttpStatus.BAD_REQUEST);
        if (!shipRepository.findById(id).isPresent())
            return new ResponseEntity(null,HttpStatus.NOT_FOUND);
        shipRepository.deleteById(id);
        return new ResponseEntity(null,HttpStatus.OK);
    }

    @PostMapping(value = "/rest/ships")
    public ResponseEntity<Ship> createShip(
        @RequestBody Ship ship
    ){

        if(     ship.getName() == null ||
                ship.getName().length() == 0 ||
                ship.getName().length() >50 ||
                ship.getPlanet() == null ||
                ship.getPlanet().length() == 0 ||
                ship.getPlanet().length() >50 ||
                ship.getShipType() == null ||
                ship.getProdDate() == null ||
                ship.getProdDate().getYear() + 1900 < 2800  ||
                ship.getProdDate().getYear() + 1900 > 3019  ||
                ship.getProdDate().getTime() < 0 ||
                ship.getSpeed() == null ||
                ((Long)Math.round(ship.getSpeed()*100)).doubleValue()/100.00  < 0.01 ||
                ((Long)Math.round(ship.getSpeed()*100)).doubleValue()/100.00  > 0.99 ||
                ship.getCrewSize() == null ||
                ship.getCrewSize() < 1 ||
                ship.getCrewSize() > 9999
          )
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);

        ship.setSpeed(((Long)Math.round(ship.getSpeed()*100)).doubleValue()/100);
        if (ship.getUsed() == null)
            ship.setUsed(false);

        ship.updateRating(nowYear);

        shipRepository.save(ship);
        return new ResponseEntity<>(ship,HttpStatus.OK);
    }

    @GetMapping(value = "/rest/ships")
    public List<Ship> showShips(
            @RequestParam(name = "name", defaultValue = "") String name,
            @RequestParam(name = "planet", defaultValue = "") String planet,
            @RequestParam(name = "shipType", defaultValue = "") ShipType shipType,
            @RequestParam(name = "after", defaultValue = "") Long after,
            @RequestParam(name = "before", defaultValue = "") Long before,
            @RequestParam(name = "isUsed", defaultValue = "") Boolean isUsed,
            @RequestParam(name = "minSpeed", defaultValue = "") Double minSpeed,
            @RequestParam(name = "maxSpeed", defaultValue = "") Double maxSpeed,
            @RequestParam(name = "minCrewSize", defaultValue = "") Integer minCrewSize,
            @RequestParam(name = "maxCrewSize", defaultValue = "") Integer maxCrewSize,
            @RequestParam(name = "minRating", defaultValue = "") Double minRating,
            @RequestParam(name = "maxRating", defaultValue = "") Double maxRating,
            @RequestParam(name = "order", defaultValue = "") ShipOrder order,
            @RequestParam(name = "pageNumber", defaultValue = "") Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = "3") Integer pageSize
    ){
        logger.info("GET");
        if(order == null) order = ShipOrder.ID;
        if(pageNumber == null) pageNumber = 0;
        List<Ship> ships =  shipRepository.findAll(Sort.by(order.getFieldName()));
        ships = filterShips(name, planet, shipType, after, before, isUsed, minSpeed, maxSpeed, minCrewSize, maxCrewSize, minRating, maxRating, ships);
        List<Ship> returnShip = new ArrayList<>();
        returnShip.clear();
        for (int i=pageNumber*pageSize;(i<ships.size()) && (i<(pageNumber+1)*pageSize);i++)
            returnShip.add(ships.get(i));
        return returnShip;
    }

    @GetMapping(value = "/rest/ships/{id}")
    public Ship getShipById(@PathVariable Long id){
        if(id <= 0)
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Provide correct Ship Id");
        try {
            Ship ship = shipRepository.findById(id).get();
            return ship;
        }catch (NoSuchElementException exp){
            throw new NotFoundException();
        }
    }

    @GetMapping(value = "/rest/ships/count")
    public Integer countShips(
            @RequestParam(name = "name", defaultValue = "") String name,
            @RequestParam(name = "planet", defaultValue = "") String planet,
            @RequestParam(name = "shipType", defaultValue = "") ShipType shipType,
            @RequestParam(name = "after", defaultValue = "") Long after,
            @RequestParam(name = "before", defaultValue = "") Long before,
            @RequestParam(name = "isUsed", defaultValue = "") Boolean isUsed,
            @RequestParam(name = "minSpeed", defaultValue = "") Double minSpeed,
            @RequestParam(name = "maxSpeed", defaultValue = "") Double maxSpeed,
            @RequestParam(name = "minCrewSize", defaultValue = "") Integer minCrewSize,
            @RequestParam(name = "maxCrewSize", defaultValue = "") Integer maxCrewSize,
            @RequestParam(name = "minRating", defaultValue = "") Double minRating,
            @RequestParam(name = "maxRating", defaultValue = "") Double maxRating
    ){
        List<Ship> ships = shipRepository.findAll();
        return filterShips(name, planet, shipType, after, before, isUsed, minSpeed, maxSpeed, minCrewSize, maxCrewSize, minRating, maxRating, ships).size();
    }

    private List<Ship> filterShips(
            String name,
            String planet,
            ShipType shipType,
            Long after,
            Long before,
            Boolean isUsed,
            Double minSpeed,
            Double maxSpeed,
            Integer minCrewSize,
            Integer maxCrewSize,
            Double minRating,
            Double maxRating,
            List<Ship> ships){
        if (name.trim().length() > 0 )
            ships = ships.stream().filter((ship) -> ship.getName().indexOf(name)>0).collect(Collectors.toList());
        if (planet.trim().length() > 0)
            ships = ships.stream().filter((ship) -> ship.getPlanet().indexOf(planet)>0).collect(Collectors.toList());
        if(after != null)
            ships = ships.stream().filter((ship) -> ship.getProdDate().getTime() >= after).collect(Collectors.toList());
        if(before != null )
            ships = ships.stream().filter((ship) -> ship.getProdDate().getTime() <= before).collect(Collectors.toList());
        if(isUsed != null )
            ships = ships.stream().filter((ship) -> ship.getUsed() == isUsed).collect(Collectors.toList());

        if(minSpeed != null )
            ships = ships.stream().filter((ship) -> ship.getSpeed() >= minSpeed).collect(Collectors.toList());
        if(maxSpeed != null )
            ships = ships.stream().filter((ship) -> ship.getSpeed() <= maxSpeed).collect(Collectors.toList());

        if(minCrewSize != null )
            ships = ships.stream().filter((ship) -> ship.getCrewSize() >= minCrewSize).collect(Collectors.toList());
        if(maxCrewSize != null )
            ships = ships.stream().filter((ship) -> ship.getCrewSize() <= maxCrewSize).collect(Collectors.toList());

        if(minRating != null )
            ships = ships.stream().filter((ship) -> ship.getRating() >= minRating).collect(Collectors.toList());
        if(maxRating != null )
            ships = ships.stream().filter((ship) -> ship.getRating() <= maxRating).collect(Collectors.toList());

        if(shipType != null )
            ships = ships.stream().filter((ship) -> ship.getShipType() == shipType).collect(Collectors.toList());

        return ships;
    }

}
