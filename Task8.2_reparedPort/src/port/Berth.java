package port;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import warehouse.Container;
import warehouse.Warehouse;

public class Berth {

	private int id;
	private Warehouse portWarehouse;

	public Berth(int id, Warehouse warehouse) {
		this.id = id;
		portWarehouse = warehouse;
	}

	public int getId() {
		return id;
	}

	public boolean add(Warehouse shipWarehouse, int numberOfConteiners) throws InterruptedException {
		boolean result = false;
		Lock portWarehouseLock = portWarehouse.getLock();	
		boolean portLock = false;

		try{
			portLock = portWarehouseLock.tryLock(30, TimeUnit.SECONDS);
			if (portLock) {
				/*int newConteinerCount = portWarehouse.getRealSize()	+ numberOfConteiners;
				if (newConteinerCount <= portWarehouse.getFreeSize()) {
					result = doMoveFromShip(shipWarehouse, numberOfConteiners);	
				}*/

				// если корабль захочет выгружать больше контейнеров чем есть в порту, то вместо этого
				// он выгрузит максимально доступное кол-во
				if(numberOfConteiners >= portWarehouse.getFreeSize() && portWarehouse.getFreeSize() != 0){
					numberOfConteiners = portWarehouse.getFreeSize();
				}

				// если места хватает, то выгрузит сколько захотел
				if (numberOfConteiners <= portWarehouse.getFreeSize()) {
					result = doMoveFromShip(shipWarehouse, numberOfConteiners);
				}
			}
		} finally{
			if (portLock) {
				portWarehouseLock.unlock();
			}
		}

		return result;
	}
	
	private boolean doMoveFromShip(Warehouse shipWarehouse, int numberOfConteiners) throws InterruptedException{
		Lock shipWarehouseLock = shipWarehouse.getLock();
		boolean shipLock = false;
		
		try{
			shipLock = shipWarehouseLock.tryLock(30, TimeUnit.SECONDS);
			if (shipLock) {
				if(shipWarehouse.getRealSize() >= numberOfConteiners){
					List<Container> containers = shipWarehouse.getContainer(numberOfConteiners);
					portWarehouse.addContainer(containers);
					return true;
				}
			}
		}finally{
			if (shipLock) {
				shipWarehouseLock.unlock();
			}
		}
		
		return false;		
	}

	public int get(Warehouse shipWarehouse, int numberOfConteiners) throws InterruptedException {
		int result = 0;
		Lock portWarehouseLock = portWarehouse.getLock();	
		boolean portLock = false;

		try{
			portLock = portWarehouseLock.tryLock(30, TimeUnit.SECONDS);
			if (portLock) {
				// Если корабль захочет взять больше чем есть в порту, то просто возьмем все что там есть
				if(numberOfConteiners > portWarehouse.getRealSize() && portWarehouse.getRealSize() != 0){
					numberOfConteiners = portWarehouse.getRealSize();
				}

				if (numberOfConteiners <= portWarehouse.getRealSize()) {
					result = doMoveFromPort(shipWarehouse, numberOfConteiners);	
				}
			}
		} finally{
			if (portLock) {
				portWarehouseLock.unlock();
			}
		}

		return result;
	}
	
	private int doMoveFromPort(Warehouse shipWarehouse, int numberOfConteiners) throws InterruptedException{
		Lock shipWarehouseLock = shipWarehouse.getLock();
		boolean shipLock = false;
		
		try{
			shipLock = shipWarehouseLock.tryLock(30, TimeUnit.SECONDS);
			if (shipLock) {
				/*int newConteinerCount = shipWarehouse.getRealSize() + numberOfConteiners;
				if(newConteinerCount <= shipWarehouse.getFreeSize()){
					List<Container> containers = portWarehouse.getContainer(numberOfConteiners);
					shipWarehouse.addContainer(containers);
					return true;
				}*/
				// Если на корабле нет места для запрашиваемого кол-ва, то возьмем сколько сможем
				if(numberOfConteiners > shipWarehouse.getFreeSize() && shipWarehouse.getFreeSize() != 0) {
					numberOfConteiners = shipWarehouse.getFreeSize();
				}
				// если места хватает, то загрузит сколько захотел
				if(numberOfConteiners <= shipWarehouse.getFreeSize()){
					List<Container> containers = portWarehouse.getContainer(numberOfConteiners);
					shipWarehouse.addContainer(containers);
					return numberOfConteiners;
				}
			}
		}finally{
			if (shipLock) {
				shipWarehouseLock.unlock();
			}
		}
		
		return 0;
	}
}
