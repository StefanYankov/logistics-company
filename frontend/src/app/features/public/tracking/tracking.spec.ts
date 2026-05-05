import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { Tracking } from './tracking';

describe('Tracking', () => {
  let component: Tracking;
  let fixture: ComponentFixture<Tracking>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Tracking],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]) // Dummy router to satisfy ActivatedRoute
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Tracking);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
