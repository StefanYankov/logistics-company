import {ComponentFixture, TestBed} from '@angular/core/testing';
import {OfficeList} from './office-list';
import {provideHttpClient} from '@angular/common/http';
import {provideHttpClientTesting} from '@angular/common/http/testing';
import {provideRouter, Router} from '@angular/router';
import {of} from 'rxjs';
import {OfficeAPIService, OfficeViewDto} from '../../../api';

describe('OfficeList', () => {
  let component: OfficeList;
  let fixture: ComponentFixture<OfficeList>;
  let mockOfficeApi: any;
  let router: Router;

  const mockOffices: OfficeViewDto[] = [
    { id: 1, cityName: 'Sofia', fullAddress: '123 Main St' },
    { id: 2, cityName: 'Plovdiv', fullAddress: '456 Oak Ave' }
  ];

  beforeEach(async () => {
    mockOfficeApi = {
      getAllOffices: vi.fn().mockReturnValue(of({ content: mockOffices })),
      deleteOffice: vi.fn().mockReturnValue(of(null))
    };

    await TestBed.configureTestingModule({
      imports: [OfficeList],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: OfficeAPIService, useValue: mockOfficeApi }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(OfficeList);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should create and load offices', () => {
    expect(component).toBeTruthy();
    expect(mockOfficeApi.getAllOffices).toHaveBeenCalled();
    expect(component.offices().length).toBe(2);
  });

  it('should call deleteOffice and reload offices on confirmation', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);

    component.deleteOffice(1);

    expect(mockOfficeApi.deleteOffice).toHaveBeenCalledWith(1);
    expect(mockOfficeApi.getAllOffices).toHaveBeenCalledTimes(2); // Initial load + reload
  });

  it('should not call deleteOffice if confirmation is cancelled', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false);

    component.deleteOffice(1);

    expect(mockOfficeApi.deleteOffice).not.toHaveBeenCalled();
  });
});
