import {Component} from '@angular/core';
import {RouterOutlet} from '@angular/router';
import {PublicHeader} from '../../shared/ui/public-header/public-header';
import {Footer} from '../../shared/ui/footer/footer';

/**
 * Shell component for all unauthenticated routes (Home, Login, Register).
 * Provides the public navigation header and standard footer.
 */
@Component({
  selector: 'app-public-layout',
  standalone: true,
  imports: [RouterOutlet, PublicHeader, Footer],
  templateUrl: './public-layout.html',
  styleUrl: './public-layout.css'
})
export class PublicLayout {}
